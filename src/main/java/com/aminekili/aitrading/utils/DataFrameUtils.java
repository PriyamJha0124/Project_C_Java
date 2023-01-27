package com.aminekili.aitrading.utils;


import com.aminekili.aitrading.service.CsvReader;
import smile.data.DataFrame;
import smile.data.formula.Formula;
import smile.data.vector.BaseVector;
import smile.data.vector.ByteVector;
import smile.data.vector.DoubleVector;
import smile.data.vector.StringVector;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

/**
 * This class handles the data set operations
 * Merging two data sets
 * Splitting a data set into two data sets
 * Normalizing a data set
 */
public class DataFrameUtils {

    public static DataFrame merge(DataFrame dataframe1, DataFrame dataframe2) {
        return dataframe1.merge(dataframe2);
    }

    public static DataFrame[] split(DataFrame dataframe, double ratio) {
        DataFrame[] dataFrames = new DataFrame[2];
        int count = dataframe.size();
        int splitIndex = (int) (count * ratio);
        dataFrames[0] = dataframe.slice(0, splitIndex);
        dataFrames[1] = dataframe.slice(splitIndex, count);
        return dataFrames;
    }

    /**
     * map string categorical values to byte values
     *
     * @param dataframe
     * @param columns
     * @return
     */
    public static Map<String, Map<String, Byte>> mapCategoricalColumns(DataFrame dataframe, String... columns) {
        Map<String, Map<String, Byte>> map = new HashMap<>();
        for (String column : columns) {
            String[] values = dataframe.column(column).toStringArray();
            for (int i = 0; i < dataframe.size(); i++) {
                byte value = dataframe.column(column).getByte(i);
                if (!map.containsKey(column)) {
                    map.put(column, new HashMap<>());
                }
                if (!map.get(column).containsKey(values[i])) {
                    map.get(column).put(values[i], value);
                }
            }
        }
        return map;
    }

    /**
     * @param dataframe
     * @param columns
     * @return
     */
    public static Map<String, Map<Byte, String>> mapValuesToCategoricalColumns(DataFrame dataframe, String... columns) {
        Map<String, Map<Byte, String>> map = new HashMap<>();
        for (String column : columns) {
            String[] values = dataframe.column(column).toStringArray();
            for (int i = 0; i < dataframe.size(); i++) {
                byte value = dataframe.column(column).getByte(i);
                if (!map.containsKey(column)) {
                    map.put(column, new HashMap<>());
                }
                if (!map.get(column).containsKey(value)) {
                    map.get(column).put(value, values[i]);
                }
            }
        }
        return map;
    }

    public static DataFrame toStringCategoricalDataFrame(DataFrame dataframe, Map<String, Map<Byte, String>> valuesPerCategoryPerColumn) {
        var columns = dataframe.names();
        var columnsVectors = new ArrayList<BaseVector>();

        for (String column : columns) {
            if (valuesPerCategoryPerColumn.containsKey(column)) {
                var valuesPerCategory = valuesPerCategoryPerColumn.get(column);
                var vector = dataframe.column(column);
                var values = new String[vector.size()];
                for (int i = 0; i < vector.size(); i++) {
                    values[i] = valuesPerCategory.get(vector.getByte(i));
                }

                columnsVectors.add(StringVector.of(column, values));
            } else {
                columnsVectors.add(dataframe.column(column));
            }
        }
        BaseVector[] vectors = new BaseVector[columnsVectors.size()];
        columnsVectors.toArray(vectors);
        return DataFrame.of(vectors);
    }

    public static DataFrame toByteCategoricalDataFrame(DataFrame dataframe, Map<String, Map<String, Byte>> categoryPerValuePerColumn) {
        var columns = dataframe.names();
        var columnsVectors = new ArrayList<BaseVector>();

        for (String column : columns) {
            if (categoryPerValuePerColumn.containsKey(column)) {
                var categoryPerValue = categoryPerValuePerColumn.get(column);
                var vector = dataframe.column(column);
                byte[] values = new byte[vector.size()];
                var vectorStringValues = vector.toStringArray();
                for (int i = 0; i < vector.size(); i++) {
                    values[i] = categoryPerValue.get(vectorStringValues[i]);
                }
                columnsVectors.add(ByteVector.of(column, values));
            } else {
                columnsVectors.add(dataframe.column(column));
            }
        }
        BaseVector[] vectors = new BaseVector[columnsVectors.size()];
        columnsVectors.toArray(vectors);
        return DataFrame.of(vectors);
    }


    public static Triplet<DataFrame, Map<String, Double>, Map<String, Double>> normalize(DataFrame dataframe) {

        Map<String, Double> min = new HashMap<>();
        Map<String, Double> max = new HashMap<>();

        // Iterate over columns
        // Find min and max values for each column
        String[] names = dataframe.names();
        for (String name : names) {
            double[] values = dataframe.column(name).toDoubleArray();
            double minVal = Double.MAX_VALUE;
            double maxVal = Double.MIN_VALUE;
            for (double value : values) {
                if (value < minVal) {
                    minVal = value;
                }
                if (value > maxVal) {
                    maxVal = value;
                }
            }
            min.put(name, minVal);
            max.put(name, maxVal);
        }

        // Iterate over columns
        // Normalize each column
        // Create a copy of dataframe
        // Put normalized values in the copy
        double[][] normalizedData = new double[dataframe.nrows()][dataframe.ncols()];
        for (var name : names) {
            double[] column = dataframe.column(name).toDoubleArray();
            for (int i = 0; i < column.length; i++) {
                double normalizedValue = (column[i] - min.get(name)) / (max.get(name) - min.get(name));
                normalizedData[i][dataframe.columnIndex(name)] = normalizedValue;
            }
        }

        var normalizedDataframe = DataFrame.of(normalizedData, names);
        return new Triplet<>(normalizedDataframe, min, max);
    }

    public static DataFrame denormalize(DataFrame dataframe, Map<String, Double> min, Map<String, Double> max) {
        double[][] denormalizedData = new double[dataframe.nrows()][dataframe.ncols()];
        String[] names = dataframe.names();
        for (var name : names) {
            double[] column = dataframe.column(name).toDoubleArray();
            for (int i = 0; i < column.length; i++) {
                double denormalizedValue = column[i] * (max.get(name) - min.get(name)) + min.get(name);
                denormalizedData[i][dataframe.columnIndex(name)] = denormalizedValue;
            }
        }
        return DataFrame.of(denormalizedData, names);
    }

    /**
     * This method reads the data set from the given path
     *
     * @param paths
     * @return
     * @throws IOException
     * @throws URISyntaxException
     */
    public static DataFrame mergeMultipleFiles(DataFrame[] paths) throws IOException, URISyntaxException {
        // Dataframe must be normalized before merging, otherwise it will cause inconsistency in the data
        //        DataFrame merged = paths[0];
        //        var normalized = normalize(merged);
        //         TODO: Identify each dataframe with a unique id, will be used to denormalize the data
        //         TODO: Verify if there is price jumps in the data, before merging
        //        for (int i = 1; i < paths.length; i++) {
        //            var normalized2 = normalize(paths[i]);
        //             TODO: denormalize multiple will make the data inconsistent, since each dataframe is normalized separately and therefore has different min and max values
        //            merged = merge(normalized, normalized2.getFirst());
        //            normalized = normalize(merged);
        //        }
        //        return merged;
        return null;
    }


    /**
     * Filter dataframe based on Predicate function
     *
     * @return filtered dataframe
     * @throws IOException
     * @throws URISyntaxException
     */
    public static DataFrame filter(DataFrame dataframe, String columnName, Predicate<Double> predicate) throws IOException, URISyntaxException {
        var columnsVectors = new ArrayList<BaseVector>();
        var columns = dataframe.names();
        List<Integer> rowsToKeep = new ArrayList<>();
        for (int i = 0; i < dataframe.nrows(); i++) {
            if (predicate.test(dataframe.column(columnName).getDouble(i))) {
                rowsToKeep.add(i);
            }
        }

        LoggingUtils.print("Rows to keep: " + (rowsToKeep.size()));


        int line = 0;
        for (String column : columns) {
            line = 0;
            var vector = dataframe.column(column);
            if (vector.field().type.isDouble()) {
                double[] newValues = new double[rowsToKeep.size()];
                for (int i = 0; i < dataframe.nrows(); i++) {
                    if (rowsToKeep.contains(i)) {
                        newValues[line] = vector.getDouble(i);
                        line++;
                    }
                }
                columnsVectors.add(DoubleVector.of(column, newValues));
            } else if (vector.field().type.isByte()) {
                byte[] newValues = new byte[rowsToKeep.size()];
                for (int i = 0; i < dataframe.nrows(); i++) {
                    if (rowsToKeep.contains(i)) {
                        newValues[line] = vector.getByte(i);
                        line++;
                    }
                }
                columnsVectors.add(ByteVector.of(column, newValues));
            } else {
                String[] values = dataframe.column(column).toStringArray();
                String[] newValues = new String[rowsToKeep.size()];
                for (int i = 0; i < dataframe.nrows(); i++) {
                    if (rowsToKeep.contains(i)) {
                        newValues[line] = values[i];
                        line++;
                    }
                }
                columnsVectors.add(StringVector.of(column, newValues));
            }
        }
        BaseVector[] vectors = new BaseVector[columnsVectors.size()];
        columnsVectors.toArray(vectors);
        return DataFrame.of(vectors);
    }

    public static void main(String... args) throws IOException, URISyntaxException {

        final Formula formula = Formula.of("EXECUTE", "WAP", "Count", "Minute", "Tesla3", "Tesla6", "Tesla9", "Decision");

        var dataframe = CsvReader.read("src/main/resources/AUD_train.csv", formula);
        LoggingUtils.print("Dataframe\n" + dataframe.toString());
        LoggingUtils.print("Dataframe\n" + dataframe.summary().toString());

        var categoricalMapDouble = DataFrameUtils.mapCategoricalColumns(dataframe, "EXECUTE", "Decision");
        LoggingUtils.print("Categorical map double\n" + categoricalMapDouble.toString());

        var categoricalMapString = DataFrameUtils.mapValuesToCategoricalColumns(dataframe, "EXECUTE", "Decision");
        LoggingUtils.print("Categorical map string\n" + categoricalMapString.toString());

        var filtered = DataFrameUtils.filter(dataframe, "Minute", val -> val == 20 || val == 40 || val == 0);
        LoggingUtils.print("Filtered\n" + filtered.toString());
        LoggingUtils.print("Filtered\n" + filtered.summary().toString());

        var formatted = formula.frame(filtered);
        LoggingUtils.print("Formatted\n" + formatted.toString());
        LoggingUtils.print("Formatted\n" + formatted.schema().toString());


        var normalizedDataframe = DataFrameUtils.normalize(dataframe);

        LoggingUtils.print("Normalized\n" + normalizedDataframe.toString());
//        LoggingUtils.print("Normalized\n" + normalizedDataframe.summary().toString());
    }

    public static void main3(String... args) throws IOException, URISyntaxException {

        final Formula formula = Formula.of("EXECUTE", "WAP", "Count", "Minute", "Tesla3", "Tesla6", "Tesla9", "Decision");

        var dataframe = CsvReader.read("src/main/resources/AUD_train.csv", formula);

        LoggingUtils.print("Initial DataFrame\n" + dataframe.toString());

        var categoricalMapDouble = DataFrameUtils.mapCategoricalColumns(dataframe, "EXECUTE", "Decision");
        LoggingUtils.print("Categorical map double\n" + categoricalMapDouble.toString());

        var categoricalMapString = DataFrameUtils.mapValuesToCategoricalColumns(dataframe, "EXECUTE", "Decision");
        LoggingUtils.print("Categorical map string\n" + categoricalMapString.toString());

        var doubleOnlyDataFrame = DataFrameUtils.toByteCategoricalDataFrame(dataframe, categoricalMapDouble);

        LoggingUtils.print("Double only dataframe\n" + doubleOnlyDataFrame.toString());

//        var normalizedDataframe = DataFrameUtils.toCategoricalDataFrame(doubleOnlyDataFrame, categoricalMapString);
//
//        LoggingUtils.print("Normalized\n" + normalizedDataframe.toString());
    }

    public static void main4(String... args) throws IOException, URISyntaxException {
        final Formula formula = Formula.of("EXECUTE", "WAP", "Count", "Minute", "Tesla3", "Tesla6", "Tesla9", "Decision");

        var dataframe = CsvReader.read("src/main/resources/AUD_train.csv", formula);

        LoggingUtils.print("Initial DataFrame\n" + dataframe.toString());

        var categoricalMapDouble = DataFrameUtils.mapCategoricalColumns(dataframe, "EXECUTE", "Decision");
        LoggingUtils.print("Categorical map double\n" + categoricalMapDouble.toString());

        var categoricalMapString = DataFrameUtils.mapValuesToCategoricalColumns(dataframe, "EXECUTE", "Decision");
        LoggingUtils.print("Categorical map string\n" + categoricalMapString.toString());

        var doubleOnlyDataFrame = DataFrameUtils.toByteCategoricalDataFrame(dataframe, categoricalMapDouble);

        LoggingUtils.print("Double only dataframe\n" + doubleOnlyDataFrame.toString());

//        var normalizedDataframe = DataFrameUtils.toCategoricalDataFrame(doubleOnlyDataFrame, categoricalMapString);
//
//        LoggingUtils.print("Normalized\n" + normalizedDataframe.toString());
    }


}
