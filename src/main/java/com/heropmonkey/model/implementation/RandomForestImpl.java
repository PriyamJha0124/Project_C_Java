package com.heropmonkey.model.implementation;

import com.heropmonkey.model.BaseModel;
import com.heropmonkey.service.CsvReader;
import com.heropmonkey.utils.ConfusionMatrix;
import com.heropmonkey.utils.DataFrameUtils;
import com.heropmonkey.utils.LoggingUtils;
import com.heropmonkey.utils.Pair;
import smile.base.cart.SplitRule;
import smile.classification.RandomForest;
import smile.data.DataFrame;
import smile.data.formula.Formula;
import smile.data.vector.BaseVector;
import smile.data.vector.ByteVector;
import smile.data.vector.DoubleVector;
import smile.math.MathEx;
import smile.validation.ClassificationValidation;

import java.io.IOException;
import java.net.URISyntaxException;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.function.Predicate;

/**
 * This class is responsible for the implementation of the Random Forest algorithm
 * it will be used to decide whether to execute flag should be set to true or false
 */
public class RandomForestImpl implements BaseModel {

    private static final int NUMBER_OF_TREES = 100;

    // initialized in static initialisation block
    public static final long[] seeds;

    private final String trainingPath;
    private final String testingPath;
    private final String modelPath;
    private RandomForest model;
    private final List<Pair<String, Predicate<Double>>> predicatePerColumn;
    private final Properties properties;
    private Map<String, Map<String, Byte>> stringCategoryMapByteCategory;
    private Map<String, Map<Byte, String>> byteCategoryMapStringCategory;

    private static final String[] inputColumns = new String[]{
            "Open", "High", "Low", "Close", "Volume",
            "WAP", "Count", "Minute", "Tesla3",
            "Tesla6", "Tesla9", "Decision"
    };


    public static final Formula formula = Formula.of("EXECUTE", "WAP", "Count", "Minute", "Tesla3", "Tesla6", "Tesla9", "Decision");


    public static void main(String... args) {

    }

    private static RandomForestImpl singleton;

    private static void activate(){
        try {
            Predicate<Double> filterMinute = val -> val == 20 || val == 40 || val == 0;
            var randomForestImpl = new RandomForestImpl(
                    "src/main/resources/AUD_train.csv", "src/main/resources/AUD_test.csv", "",
                    List.of(
                            new Pair<>("Minute", filterMinute)
                    )
            );
            randomForestImpl.train();
            singleton = randomForestImpl;
        } catch (IOException | URISyntaxException e) {
            e.printStackTrace();
        }
    }

    public static RandomForestImpl getInstance(){
        if (singleton == null){
            activate();
        }
      return singleton;
    }

    private RandomForestImpl(String trainDataSetPath, String testDateSetPath, String modelPath, List<Pair<String, Predicate<Double>>> predicatePerColumn) throws IOException, URISyntaxException {
        MathEx.setSeed(19650218);
        this.trainingPath = trainDataSetPath;
        this.testingPath = testDateSetPath;
        this.modelPath = modelPath;
        this.predicatePerColumn = predicatePerColumn;
        properties = new java.util.Properties();
        properties.setProperty("smile.random.forest.trees", String.valueOf(NUMBER_OF_TREES));
    }

    /**
     * This method will train the model using the training data set
     *
     * @throws IOException
     * @throws URISyntaxException
     */
    public void train() throws IOException, URISyntaxException {
        DataFrame byteOnlyData = getDataFrameReady(trainingPath, "TRAINING");

        var forest = RandomForest.fit(formula, byteOnlyData, properties);

        LoggingUtils.print("Model metrics: " + forest.metrics());
        this.model = forest;
    }

    /**
     * This method will evaluate the model precision using the testing data set
     *
     * @throws IOException
     * @throws URISyntaxException
     */
    public void test() throws IOException, URISyntaxException {
        var dataFrame = getDataFrameReady(testingPath, "TEST");

        var predictedStr = new String[dataFrame.size()];
        var actualStr = new String[dataFrame.size()];

        for (int i = 0; i < dataFrame.size(); i++) {
            var row = dataFrame.get(i);
            var prediction = model.predict(row);
            var predictedStringCategory = byteCategoryMapStringCategory.get("EXECUTE").getOrDefault(Integer.valueOf(prediction).byteValue(), "NONE");
            var actualStringCategory = byteCategoryMapStringCategory.get("EXECUTE").get(row.getByte("EXECUTE"));
            predictedStr[i] = predictedStringCategory;
            actualStr[i] = actualStringCategory;
            LoggingUtils.print(MessageFormat.format("Prediction: {0} - Actual: {1}", predictedStringCategory, actualStringCategory));
        }

        var confusionMatrix = new ConfusionMatrix(predictedStr, actualStr);
        LoggingUtils.print(confusionMatrix.toString());
    }

    /**
     * Evaluate the precision of the model
     *
     * @throws IOException
     * @throws URISyntaxException
     */
    public void evaluateModelPrecision() throws IOException, URISyntaxException {
        DataFrame byteOnlyTrainingData = getDataFrameReady(trainingPath, "Evaluation");
        DataFrame byteOnlyTestData = getDataFrameReady(testingPath, "Evaluation");


        var classificationValidation = ClassificationValidation.of(formula, byteOnlyTrainingData, byteOnlyTestData,
                (f, x) -> RandomForest.fit(f, x, 20, 2, SplitRule.GINI, 2, 10, 1, 1.0, new int[]{1, 100}, Arrays.stream(seeds))
        );

        LoggingUtils.print(MessageFormat.format("Evaluation metrics = {0}", classificationValidation.toString()));
        int[] truth = classificationValidation.truth;
        int[] prediction = classificationValidation.prediction;
        for (int i = 0; i < truth.length; i++) {
            var predictedStringCategory = byteCategoryMapStringCategory.get("EXECUTE").getOrDefault(Integer.valueOf(prediction[i]).byteValue(), "NONE");
            var actualStringCategory = byteCategoryMapStringCategory.get("EXECUTE").getOrDefault(Integer.valueOf(truth[i]).byteValue(), "NONE");
            LoggingUtils.print(MessageFormat.format("Prediction: {0} - Actual: {1}", predictedStringCategory, actualStringCategory));
        }
        System.out.println(MessageFormat.format("Confusion Matrix = {0}", classificationValidation.confusion.toString()));
    }

    private DataFrame getDataFrameReady(String path, String phase) throws IOException, URISyntaxException {
        var trainingData = CsvReader.read(path, formula);

        stringCategoryMapByteCategory = DataFrameUtils.mapCategoricalColumns(trainingData, "EXECUTE", "Decision");
        byteCategoryMapStringCategory = DataFrameUtils.mapValuesToCategoricalColumns(trainingData, "EXECUTE", "Decision");

        var byteOnlyData = DataFrameUtils.toByteCategoricalDataFrame(trainingData, stringCategoryMapByteCategory);
        byteOnlyData = formula.frame(byteOnlyData);

        LoggingUtils.format("{0} | Before Filter, data size: {1}", phase, byteOnlyData.size());
        for (var pair : predicatePerColumn) {
            byteOnlyData = DataFrameUtils.filter(byteOnlyData, pair.getFirst(), pair.getSecond());
        }
        LoggingUtils.format("{0} | After Filter, data size: {1}", phase, byteOnlyData.size());

        byteOnlyData = formula.frame(byteOnlyData);

        LoggingUtils.print("Schema: " + byteOnlyData.schema());
        LoggingUtils.print("Formula: " + formula);

        return byteOnlyData;
    }

    /**
     * This method will be used to predict the decision of the algorithm
     *
     * @param wap:    wap
     * @param volume: volume
     * @param count:  trades count
     * @param minute: minute
     * @param tesla3: tesla3
     * @param tesla6: tesla6
     * @param tesla9: tesla9
     */
    public String predict(double open, double high, double low, double close, double wap, double volume, double count, double minute, double tesla3, double tesla6, double tesla9, String decision) {
        if (model == null) {
            throw new IllegalStateException("Model is not trained yet");
        }

        Byte decision_byte = stringCategoryMapByteCategory.get("Decision").get(decision);

        var vector = new BaseVector[inputColumns.length];

        vector[0] = DoubleVector.of("Open", new double[]{open});
        vector[1] = DoubleVector.of("High", new double[]{high});
        vector[2] = DoubleVector.of("Low", new double[]{low});
        vector[3] = DoubleVector.of("Close", new double[]{close});

        vector[4] = DoubleVector.of("WAP", new double[]{wap});
        vector[5] = DoubleVector.of("Volume", new double[]{volume});
        vector[6] = DoubleVector.of("Count", new double[]{count});
        vector[7] = DoubleVector.of("Minute", new double[]{minute});
        vector[8] = DoubleVector.of("Tesla3", new double[]{tesla3});
        vector[9] = DoubleVector.of("Tesla6", new double[]{tesla6});
        vector[10] = DoubleVector.of("Tesla9", new double[]{tesla9});
        vector[11] = ByteVector.of("Decision", new byte[]{decision_byte});

        var dataFrame = DataFrame.of(vector);


        var prediction = model.predict(dataFrame)[0];

        return byteCategoryMapStringCategory.get("EXECUTE").getOrDefault(Integer.valueOf(prediction).byteValue(), "NONE");
    }


    static {
        seeds = new long[]{
                342317953, 521642753, 72070657, 577451521, 266953217, 179976193,
                374603777, 527788033, 303395329, 185759582, 261518209, 461300737,
                483646580, 532528741, 159827201, 284796929, 655932697, 26390017,
                454330473, 867526205, 824623361, 719082324, 334008833, 699933293,
                823964929, 155216641, 150210071, 249486337, 713508520, 558398977,
                886227770, 74062428, 670528514, 701250241, 363339915, 319216345,
                757017601, 459643789, 170213767, 434634241, 414707201, 153100613,
                753882113, 546490145, 412517763, 888761089, 628632833, 565587585,
                175885057, 594903553, 78450978, 212995578, 710952449, 835852289,
                415422977, 832538705, 624345857, 839826433, 260963602, 386066438,
                530942946, 261866663, 269735895, 798436064, 379576194, 251582977,
                349161809, 179653121, 218870401, 415292417, 86861523, 570214657,
                701581299, 805955890, 358025785, 231452966, 584239408, 297276298,
                371814913, 159451160, 284126095, 896291329, 496278529, 556314113,
                31607297, 726761729, 217004033, 390410146, 70173193, 661580775,
                633589889, 389049037, 112099159, 54041089, 80388281, 492196097,
                912179201, 699398161, 482080769, 363844609, 286008078, 398098433,
                339855361, 189583553, 697670495, 709568513, 98494337, 99107427,
                433350529, 266601473, 888120086, 243906049, 414781441, 154685953,
                601194298, 292273153, 212413697, 568007473, 666386113, 712261633,
                802026964, 783034790, 188095005, 742646355, 550352897, 209421313,
                175672961, 242531185, 157584001, 201363231, 760741889, 852924929,
                60158977, 774572033, 311159809, 407214966, 804474160, 304456514,
                54251009, 504009638, 902115329, 870383757, 487243777, 635554282,
                564918017, 636074753, 870308031, 817515521, 494471884, 562424321,
                81710593, 476321537, 595107841, 418699893, 315560449, 773617153,
                163266399, 274201241, 290857537, 879955457, 801949697, 669025793,
                753107969, 424060977, 661877468, 433391617, 222716929, 334154852,
                878528257, 253742849, 480885528, 99773953, 913761493, 700407809,
                483418083, 487870398, 58433153, 608046337, 475342337, 506376199,
                378726401, 306604033, 724646374, 895195218, 523634541, 766543466,
                190068097, 718704641, 254519245, 393943681, 796689751, 379497473,
                50014340, 489234689, 129556481, 178766593, 142540536, 213594113,
                870440184, 277912577};
    }


}

