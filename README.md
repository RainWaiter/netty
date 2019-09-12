# Netty Project

### 在IDea中install

选择maven版本为3.3.9 （IDea自带的maven去安装，可能有点问题）

命令：
````
install -rf :netty-codec-stomp -Dcheckstyle.skip=true -Dmaven.test.skip=true -X
````

-rf :netty-codec-stomp 为从哪个项目开始执行，会跳过该项目之前的项目（通常是前面的项目已经成功install，需要跳过前面的项目）


### 在IDea中install出现的问题
<li> 出现类似oldVersion字样的问题
````
[ERROR] Failed to execute goal com.github.siom79.japicmp:japicmp-maven-plugin:0.9.2:cmp (check-api-compatibility-for-semantic-versioning) on project foo-api: Please provide at least one old version. -> [Help 1]
````

解决办法：

参考：https://github.com/siom79/japicmp/issues/153

在pom.xml中找到指定的plugin，新增参数标签：
````
<ignoreMissingOldVersion>true</ignoreMissingOldVersion>
````

修改结果如下：

````
<plugin>
    <groupId>com.github.siom79.japicmp</groupId>
    <artifactId>japicmp-maven-plugin</artifactId>
    <version>0.13.1</version>
    <configuration>
      <parameter>
        <breakBuildOnBinaryIncompatibleModifications>true</breakBuildOnBinaryIncompatibleModifications>
        <breakBuildOnSourceIncompatibleModifications>true</breakBuildOnSourceIncompatibleModifications>
        <oldVersionPattern>\d+\.\d+\.\d+\.Final</oldVersionPattern>
        <ignoreMissingClassesByRegularExpressions>
          <!-- ignore everything which is not part of netty itself as the plugin can not handle optional dependencies -->
          <ignoreMissingClassesByRegularExpression>^(?!io\.netty\.).*</ignoreMissingClassesByRegularExpression>
          <ignoreMissingClassesByRegularExpression>^io\.netty\.internal\.tcnative\..*</ignoreMissingClassesByRegularExpression>
        </ignoreMissingClassesByRegularExpressions>
        <excludes>
          <exclude>@io.netty.util.internal.UnstableApi</exclude>
        </excludes>
        <ignoreMissingOldVersion>true</ignoreMissingOldVersion>
      </parameter>
      <skip>${skipJapicmp}</skip>
    </configuration>
    <executions>
      <execution>
        <phase>verify</phase>
        <goals>
          <goal>cmp</goal>
        </goals>
      </execution>
    </executions>
</plugin>
````


