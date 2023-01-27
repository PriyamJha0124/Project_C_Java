package com.aminekili.aitrading.service;

import org.apache.commons.csv.CSVFormat;
import smile.data.DataFrame;
import smile.data.formula.Formula;
import smile.data.measure.NominalScale;
import smile.data.type.DataTypes;
import smile.data.type.StructField;
import smile.data.type.StructType;
import smile.io.Read;

import java.io.IOException;
import java.net.URISyntaxException;

public class CsvReader {

    public static StructType schema = DataTypes.struct(
            new StructField("Open", DataTypes.DoubleType),
            new StructField("High", DataTypes.DoubleType),
            new StructField("Low", DataTypes.DoubleType),
            new StructField("Close", DataTypes.DoubleType),
            new StructField("Volume", DataTypes.DoubleType),
            new StructField("WAP", DataTypes.DoubleType),
            new StructField("Count", DataTypes.DoubleType),
            new StructField("Minute", DataTypes.DoubleType),
            new StructField("Tesla3", DataTypes.DoubleType),
            new StructField("Tesla6", DataTypes.DoubleType),
            new StructField("Tesla9", DataTypes.DoubleType),
            new StructField("Decision", DataTypes.ByteType, new NominalScale("NO", "BUY", "SELL")),
            new StructField("EXECUTE", DataTypes.ByteType, new NominalScale("NO", "EXECUTE"))
    );

    public static DataFrame read(String path) throws IOException, URISyntaxException {


        return Read.csv(path, CSVFormat.DEFAULT.withFirstRecordAsHeader(), schema);
    }

    public static DataFrame read(String path, Formula formula) throws IOException, URISyntaxException {
        var dataframe = read(path);
        return formula.frame(dataframe);
    }

}
