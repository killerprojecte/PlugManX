# 警告
* 请不要声称此汉化是您的
* 本汉化仅属于制作者
* 请不要对他人提供PlugManX汉化版的本体文件
* 唯一可用的下载方式仅为[本项目Github地址](https://github.com/killerprojecte/PlugManX/)

# 介绍
* PlugMan是一个十分有用的工具类插件 能让您在调试某个插件时无需重启
* 此插件对开发者和服务器管理员 来说是一个很好的工具
* （注意: 并非每个插件都支持PlugMan的热(加载/重载/卸载/重启) 领地插件和基础插件请不要尝试)
* 非正常使用该插件造成的问题作者(不包括汉化作者)不会承担任何责任
* 发现Bug请及时提交Issue [作者Github](https://github.com/TheBlackEntity/PlugMan/)
# 构建

* [Gradle](https://gradle.org/) - 本插件使用Gradle构建
* 本项目采用MIT协议开源
* PlugManX汉化版本

GradleWrapper 已内置在本项目中

**视窗(DOS):**

```
gradlew.bat clean build shadowjar
```

**视窗(PowerShell):**

```
./gradlew.bat clean build shadowjar
```

**macOS(苹果系统)/Linux:**

```
./gradlew clean build shadowjar
```

构建工件将会保存至 `./build/libs` 文件夹.
