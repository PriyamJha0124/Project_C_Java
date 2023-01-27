# Project_C_Java 

> Project_C_Java Uses Random Forest Model to classify trading Signal from a live broker.


Given a set of inputs

- Prices ['open','close', 'high', 'low', 'wap']
- Volume
- Count: trades count during the time period
- Minute
- Tesla magic numbers ['tesla3', 'tesla6', 'tesla9']
- Decision: flag can be NO, BUY or SELL
- Execute: flag execute the decision or ignore it, can be EXECUTE or No.

## Tech

Project_Java_C uses a number of open source projects to work properly:

- [SMILE](https://haifengl.github.io/classification.html) - Statistical Machine Intelligence and Learning Engine
- [Project_A_consolidate](https://github.com/CoderOfJava8888/Project_A_consolidate) - Connects to broker derives
- [Trading Classifier](https://github.com/amineKili/ClassificationForTrading) - Trading Classifier

## Installation

Project_Java_C requires **Java 17** and **Maven** to run.

To build the project

```sh
mvn clean install
```

## Credits

- [CoderOfJava8888](https://github.com/CoderOfJava8888)
- [amineKili](https://github.com/amineKili)
