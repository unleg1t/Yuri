# Agent Guide: Yuri Client

This document provides essential information for AI agents working on the Yuri Client codebase.

## Overview

Yuri is a clean, Optifine 1.8.9-based Minecraft hacked client. It is built using Java and managed with Gradle. The project focuses on smooth visuals and effective anti-cheat bypasses.

## Getting Started

### Prerequisites

- **Java JDK 1.8** (or higher)
- **Gradle** (via included `gradlew`)
- **IDE**: IntelliJ IDEA is highly recommended.

### Development Workflow

To develop and run the client locally:

1. **Open Project**: Import the project into your IDE.
2. **Setup Run Directory**: Create a directory named `run` in the project root.
3. **Run Configuration**:
   - **Main Class**: `Start` (Note: `Start` is a wrapper that sets up natives and calls the Minecraft main class).
   - **Working Directory**: Must point to the `run` directory (e.g., `.../Yuri/run`).
   - **Classpath**: Use the classpath of the `Yuri.main` module.
4. **Run**: Execute the `Start.main()` method.

### Essential Commands

- `./gradlew shadowJar`: Builds the fat JAR (`Yuri.jar`) used for distribution.
- `./gradlew build`: Standard Gradle build task.

## Architecture & Core Patterns

### Core Singleton
The central entry point for the client logic is `ddlc.yuri.Yuri.INSTANCE`.

### Event System
The client uses a decoupled event-driven architecture via an `EventBus`.
- **Subscribing**: Use the `@EventHook` annotation on methods within classes that subscribe to the `EventBus`.
- **Posting**: Events are dispatched using `Yuri.INSTANCE.getEventBus().post(event)`.
- **Lifecycle**: Many components (including Modules) subscribe to and unsubscribe from the `EventBus` during their lifecycle.

### Module System
Most client features are implemented as `Module`s.
- **Base Class**: `ddlc.yuri.modules.Module`.
- **Metadata**: Modules are defined using the `@ModuleInfo` annotation (provides label, description, category, etc.).
- **Properties**: Modules use a property-based system for configurable settings (e.g., `NumberProperty`, `ModeProperty`, `MultiModeProperty`).
  - **Reflection**: The `Module` class uses reflection in `reflectProperties()` to automatically discover and manage these properties. Ensure properties are declared as fields in your module class.
- **Lifecycle**: Override `onEnable()` and `onDisable()` to handle logic when the module is toggled.
- **Events**: Toggling a module dispatches a `ModuleEvent`.

### Configuration Management
Configuration is handled through specialized manager classes (e.g., `ConfigManager`) and specific config models (e.g., `VisualsConfig`, `BindsConfig`).
- **Format**: Configuration is typically stored as JSON.
- **Persistence**: Modules implement `Serializable` and have `save()`/`load()` methods to persist their state and properties.

### User Interface
The client supports multiple ClickGUI implementations (e.g., `YuriClickGUI`, `NovolineClickGui`, `ImGuiClickGui`), which are toggled via the `ClickGUIModule`.

## Coding Conventions

- **Lombok**: The project uses [Lombok](https://projectlombok.org/) to reduce boilerplate. Use `@Getter`, `@Setter`, and other annotations where appropriate.
- **Naming**: Follow the existing naming conventions for packages (`ddlc.yuri.*`) and classes.
- **Decoupling**: Prefer using the `EventBus` over direct method calls between disparate systems to maintain a clean architecture.

## Important Gotchas

- **Main Class Discrepancy**: While the `shadowJar` manifest points to `net.minecraft.client.main.Main`, **always use `Start` as the entry point for development**. `Start` correctly configures the `org.lwjgl.librarypath` and passes necessary Minecraft arguments.
- **Reflection Requirement**: Because properties are discovered via reflection, they *must* be declared as class fields in your `Module` subclass to be properly registered and saved.
- **Run Directory**: The application relies heavily on a `run` directory for assets and natives. Failure to set the working directory correctly in your IDE will cause startup failures.
