# Ranked-Retrieval
Following are the commands to run:



javac PostingFileEntry.java
javac DocumentFrequency.java
javac DictionaryEntryTerm.java
javac QueryDictionaryEntry.java
javac -classpath .:/usr/local/corenlp341/stanford-corenlp-3.4.1.jar:/usr/local/corenlp341/stanford-corenlp-3.4.1-models.jar Lemmatizer.java
javac IndexBuilding.java
javac -classpath .:/usr/local/corenlp341/stanford-corenlp-3.4.1.jar:/usr/local/corenlp341/stanford-corenlp-3.4.1-models.jar QueryReader.java

IndexBuilding.java is the main file to run.
Use following command to run it:
java -classpath .:/usr/local/corenlp341/stanford-corenlp-3.4.1.jar:/usr/local/corenlp341/stanford-corenlp-3.4.1-models.jar IndexBuilding <Cranfield dataset-path> <queries file> <stopwords file-path>

Example:
java -classpath .:/usr/local/corenlp341/stanford-corenlp-3.4.1.jar:/usr/local/corenlp341/stanford-corenlp-3.4.1-models.jar IndexBuilding Cranfield hw3.queries stopwords.txt

