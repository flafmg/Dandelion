<p align="center">
  <img src="https://static.wikia.nocookie.net/minecraft-battles/images/b/b1/Dandelion.png/revision/latest?cb=20230428002404" alt="Dandelion Icon" width="48" height="48" style="vertical-align:middle;"/>
  <strong style="font-size: 1.4em; vertical-align: middle;"> Dandelion Classic</strong>
</p>

> *A classic Minecraft server software made in Kotlin!*

---

## 🧾 About

**Dandelion** is a **Minecraft Classic** server software written in **Kotlin**, focused on simplicity and customization.  
It's designed to be a lightweight and modern alternative to existing server softwares, making it easier to build and run your own custom classic servers!

> ⚠️ Still under development!  
> Many important features are missing — **not recommended for real use yet**.

## 🛠️ How to Use

1. Clone the repository:
   ```bash
   git clone https://github.com/flafmg/dandelion.git
   cd dandelion
   ```

2. Build the `.jar` using Gradle:
   ```bash
   ./gradlew clean build
   ```

3. Run the generated `.jar`:
   ```bash
   java -jar build/libs/dandelion-<version>.jar
   ```
   > 🔧 Replace `<version>` with the actual version number (e.g., `dandelion-1.0.jar`).  
   > You can check the exact name in the `build/libs/` directory.

4. The server should now be running! 🎉

---

## ✅ TO DO

- [x] Basic protocol support  
- [x] Command system  
- [x] Level system  
- [x] implement base cmmands
- [ ] *implement permission system
- [x] Event system 
- [ ] [CPE (Classic Protocol Extensions)](https://minecraft.wiki/w/Minecraft_Wiki:Projects/wiki.vg_merge/Classic_Protocol_Extension) support  
- [ ] Plugin system  

---

## 💡 Goal

The goal of Dandelion is to be a simple and cohesive codebase for Minecraft Classic servers.  
It aims to be an easier and cleaner alternative to existing server softwares, but still far from it at this development stage

---