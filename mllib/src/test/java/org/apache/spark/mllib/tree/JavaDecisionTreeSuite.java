/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.spark.mllib.tree;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.api.java.function.Function;
import org.apache.spark.mllib.linalg.Vector;
import org.apache.spark.mllib.regression.LabeledPoint;
import org.apache.spark.mllib.tree.configuration.Algo;
import org.apache.spark.mllib.tree.configuration.Strategy;
import org.apache.spark.mllib.tree.impurity.Gini;
import org.apache.spark.mllib.tree.model.DecisionTreeModel;
import org.apache.spark.sql.SparkSession;


public class JavaDecisionTreeSuite implements Serializable {
  private transient SparkSession spark;
  private transient JavaSparkContext jsc;

  @Before
  public void setUp() {
    spark = SparkSession.builder()
      .master("local")
      .appName("JavaDecisionTreeSuite")
      .getOrCreate();
    jsc = new JavaSparkContext(spark.sparkContext());
  }

  @After
  public void tearDown() {
    spark.stop();
    spark = null;
  }

  int validatePrediction(List<LabeledPoint> validationData, DecisionTreeModel model) {
    int numCorrect = 0;
    for (LabeledPoint point : validationData) {
      Double prediction = model.predict(point.features());
      if (prediction == point.label()) {
        numCorrect++;
      }
    }
    return numCorrect;
  }

  @Test
  public void runDTUsingConstructor() {
    List<LabeledPoint> arr = DecisionTreeSuite.generateCategoricalDataPointsAsJavaList();
    JavaRDD<LabeledPoint> rdd = jsc.parallelize(arr);
    HashMap<Integer, Integer> categoricalFeaturesInfo = new HashMap<>();
    categoricalFeaturesInfo.put(1, 2); // feature 1 has 2 categories

    int maxDepth = 4;
    int numClasses = 2;
    int maxBins = 100;
    Strategy strategy = new Strategy(Algo.Classification(), Gini.instance(), maxDepth, numClasses,
      maxBins, categoricalFeaturesInfo);

    DecisionTree learner = new DecisionTree(strategy);
    DecisionTreeModel model = learner.run(rdd.rdd());

    int numCorrect = validatePrediction(arr, model);
    Assert.assertTrue(numCorrect == rdd.count());
  }

  @Test
  public void runDTUsingStaticMethods() {
    List<LabeledPoint> arr = DecisionTreeSuite.generateCategoricalDataPointsAsJavaList();
    JavaRDD<LabeledPoint> rdd = jsc.parallelize(arr);
    HashMap<Integer, Integer> categoricalFeaturesInfo = new HashMap<>();
    categoricalFeaturesInfo.put(1, 2); // feature 1 has 2 categories

    int maxDepth = 4;
    int numClasses = 2;
    int maxBins = 100;
    Strategy strategy = new Strategy(Algo.Classification(), Gini.instance(), maxDepth, numClasses,
      maxBins, categoricalFeaturesInfo);

    DecisionTreeModel model = DecisionTree$.MODULE$.train(rdd.rdd(), strategy);

    // java compatibility test
    JavaRDD<Double> predictions = model.predict(rdd.map(new Function<LabeledPoint, Vector>() {
      @Override
      public Vector call(LabeledPoint v1) {
        return v1.features();
      }
    }));

    int numCorrect = validatePrediction(arr, model);
    Assert.assertTrue(numCorrect == rdd.count());
  }

}
