# Программа для поиска копий себя в сети
Программа ищет копии себя в сети используя мультикаст сервер.<br/>
Использует сервер с адресом "224.1.1.1". Если сервер недоступен, то ничего работать не будет.<br/>

# Запуск
Нужен Gradle и java. <br/>
Для запуска на Windows нужно открыть консольку, перейти в папку и прописать команду:<br/>

 **gradlew run**<br/>

 Для запуска нескольких приложений с одного устройсва, нужно открыть несколько консолеек.<br/>
 Если запускать на Linux, то надо перейти в папку и прописать следующую команду:<br/>

 **./gradlew run** <br/>

Если нужно много копий, то:<br/>

 **./gradlew run & ./gradlew run & ./gradlew run** <br/>
