# Poisonous Potato Update — Jerotes 兼容修复工具

用于给官方 `reimaginingpotatoes-1.20.1-1.0.1-forge.jar` 应用毒马铃薯僵尸与 JerotesWarehouse 的索敌兼容修复。

本工具不包含、不会下载原 Mod。公开发布修改后的完整 Mod 前，请另行确认原作者的再分发许可。

## 修复内容

- 索敌目标不是 `Player` 时，不再强制转换成 `Player`，避免 `JerotesPlayerEntity` 触发 `ClassCastException`。
- 真正的玩家仍检查毒马铃薯 Headpiece，保留佩戴后不被主动索敌的功能。
- Mod ID 保持 `reimaginingpotatoes`，物品、方块、生物等注册 ID 不变。
- 显示名称改为 `Poisonous Potato Update (Jerotes Compatibility Fix)`，版本号改为 `1.0.1-jerotes-fix-1`。

## 使用方法（Windows）

1. 从官方项目下载 Forge 1.20.1 的 1.0.1 JAR：<https://modrinth.com/mod/reimagining-potatoes/version/kOJ79Oqf>
2. 把官方 JAR 拖到 `apply-fix.bat` 上。
3. 工具校验原文件的 SHA-256，成功后会在原文件旁生成：
   - `reimaginingpotatoes-1.20.1-1.0.1-jerotes-fix.jar`：改进版；
   - `原文件名.original-backup`：未修改备份。
4. 备份存档。把改进版放入 `mods`，并确保原版没有同时以 `.jar` 文件存在。

也可以在命令行运行：

```bat
apply-fix.bat "C:\path\to\reimaginingpotatoes-1.20.1-1.0.1-forge.jar"
```

其他系统安装 Java 17 或更高版本后运行：

```sh
java -jar PoisonousPotatoJerotesFixer.jar /path/to/reimaginingpotatoes-1.20.1-1.0.1-forge.jar
```

## 限制与安全检查

- 只接受官方 1.0.1 文件，SHA-256 必须为 `74f04831df568e06ddb6e2de14362e8d32d2d9e67e422cc6b61cbadb6b55295a`。
- 不支持的版本、已修改文件或不匹配的备份会被拒绝。
- 不覆盖已存在的输出或备份。
- 工具只修改目标 class、`mods.toml` 和 JAR 清单。
- 工具针对 Minecraft 1.20.1 Forge 与 JerotesWarehouse 1.3.3 的冲突场景验证；其他版本不保证适用。

## 从源码构建

需要 JDK 17+ 和 ASM 9.x。`src/PatchPoisonousPotato.java` 是完整补丁源码。可执行 JAR 内包含 ASM 运行库，ASM 使用 BSD 3-Clause 许可证，详见 `LICENSE-ASM.txt`。
