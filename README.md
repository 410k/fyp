# tbc452's FYP: Extracting Key Phrases and Relations from Scientific Publications

Taking on the [ScienceIE](https://scienceie.github.io/) task.

## Set up and run
### Prerequisites
* Java 8 (64-bit)
* Maven 3
* For Word2Vec, some [pretrained models](https://github.com/3Top/word2vec-api) (download `Google News`, `Freebase IDs`, `Freebase Names` and `DBPedia vectors (wiki2vec)`).

### What to run
* Firstly, cd to the root of the Java code:
```
cd java/FYP/
```
* The system can be built by executing:
```
./build.sh 
```
* To run tests, execute the following. Test classes can be found under `java/FYP/src/test/java`.
```
./test.sh <test class name>
```

## Results
As of 18.12.2017
### Task 1
#### SVM
```
Overall statistics (gen): Accuracy: 0.92321915 Precision: 0.89226759 Recall: 0.73287345 F1: 0.80475382
Overall statistics (inc): Accuracy: 0.89303223 Precision: 0.78783593 Recall: 0.60559935 F1: 0.68480098
Overall statistics (str): Accuracy: 0.83590440 Precision: 0.31666667 Recall: 0.20171499 F1: 0.24644550
```
#### SVM - with Word2Vec
Using Google News:
```
Overall statistics (gen): Accuracy: 0.90517241 Precision: 0.84073403 Recall: 0.78556308 F1: 0.81221274
Overall statistics (inc): Accuracy: 0.86898336 Precision: 0.72829498 Recall: 0.68326791 F1: 0.70506329
Overall statistics (str): Accuracy: 0.77861141 Precision: 0.18391324 Recall: 0.17550668 F1: 0.17961165
```
Using Freebase names AND Freebase IDs (identical results):
```
Overall statistics (gen): Accuracy: 0.91859574 Precision: 0.89226759 Recall: 0.71617852 F1: 0.79458414
Overall statistics (inc): Accuracy: 0.88922933 Precision: 0.78783593 Recall: 0.59239564 F1: 0.67627865
Overall statistics (str): Accuracy: 0.83886618 Precision: 0.31666667 Recall: 0.20721477 F1: 0.25050710
```
Wiki2Vec wouldn't work...
### Task 2
#### SVM 
```
Overall statistics (gen): Accuracy: 0.90511886 Precision: 0.49532710 Recall: 0.02314410 F1: 0.04422194
Overall statistics (inc): Accuracy: 0.90489209 Precision: 0.43000000 Recall: 0.01884312 F1: 0.03610411
Overall statistics (str): Accuracy: 0.90434350 Precision: 0.22500000 Recall: 0.00795053 F1: 0.01535836
```
#### SVM - with Word2Vec
Using Google News:
```
Overall statistics (gen): Accuracy: 0.90625261 Precision: 0.69142857 Recall: 0.05224525 F1: 0.09714974
Overall statistics (inc): Accuracy: 0.90453540 Precision: 0.47200000 Recall: 0.02586585 F1: 0.04904406
Overall statistics (str): Accuracy: 0.90336450 Precision: 0.21428571 Recall: 0.00929615 F1: 0.01781926
```
### Task 3
#### SVM based on Word2Vec (sum of tokens in KP)
```
Overall statistics: Accuracy: 0.99417070 Precision: 0.13402062 Recall: 0.06341463 F1: 0.08609272
Specific results were: tp: 13.0 fp: 84.0 tn: 47058.0 fn: 192.0
```
#### SVM based on Word2Vec (vector of root noun in KP)
```
Overall statistics: Accuracy: 0.99507961 Precision: 0.06666667 Recall: 0.00966184 F1: 0.01687764
Specific results were: tp: 2.0 fp: 28.0 tn: 47119.0 fn: 205.0
```
