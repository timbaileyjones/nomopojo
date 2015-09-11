rm -rf target/nomopojo-1.00.00-SNAPSHOT target/nomopojo-1.00.00-SNAPSHOT.war bin
#
# maven debug flags are '-e -X'
#
#mvn -e -X clean compile package war:war wls:redeploy -DmiddlewareHome=${WL_HOME-/usr/local/wls12130} -Dverbose=true -Duser=weblogic -Dpassword=weblogic123 -Dname=nomopojo -Dsource=./target/nomopojo-1.00.00-SNAPSHOT.war
mvn -e -X clean package wls:redeploy -DmiddlewareHome=${WL_HOME-/usr/local/wls12130} -Dverbose=true -Duser=weblogic -Dpassword=weblogic123 -Dname=nomopojo -Dsource=./target/nomopojo-1.00.00-SNAPSHOT.war

