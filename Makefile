all:
	kotlinc server.kt -include-runtime -d server.jar -cp gson-2.6.2.jar:.

run:
	java -classpath .:gson-2.6.2.jar:server.jar ServerKt
