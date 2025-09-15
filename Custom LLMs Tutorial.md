# Integrating Custom LLMs into the BDDTestGen Plugin

This document outlines the process for integrating a custom Large Language Model (a.k.a. LLM) into the BDDTestGen plugin. By following these instructions, you can configure the plugin to use any LLM that can be operated via a command-line script.

## Overview

The plugin's architecture is designed to be extensible, allowing users to connect to virtually any LLM. This is achieved by defining a clear interface between the plugin and the LLM's execution logic. The integration relies on two primary components that you, the user, must provide:

1.  **A Specification File (`.json`)**: This file defines the configuration parameters for your LLM and dictates how the UI in the plugin's settings will be rendered.
2.  **An Execution Script**: This is a command-line script (e.g., in Python, Bash, PowerShell) that the plugin calls to run the LLM. It acts as a bridge between the plugin and the LLM's API.

## Core Components

Before diving into the setup, it's essential to understand the roles of these two components.

| Component | Purpose |
| :--- | :--- |
| **Specification File (`.json`)** | Defines the user interface for your LLM's settings in IntelliJ. It lists all configurable parameters, their types, and how they should be displayed (e.g., text fields, checkboxes, dropdowns). |
| **Execution Script** | Receives the configuration values from the UI as command-line arguments. It is responsible for making the API call to the LLM and saving the output to a specified file path. |

---

<details>
<summary><h2>1. The Specification File (`.json`)</h2></summary>

This JSON file is a list of objects, where each object defines a single configuration parameter for your LLM.

### Structure and Fields

Each object in the JSON array must contain the following fields:

| Field | Type | Required | Description |
| :--- | :--- | :--- | :--- |
| `name` | String | Yes | The display name of the parameter in the plugin's UI (e.g., "API Key"). |
| `argName` | String | Yes | The command-line argument that corresponds to this parameter (e.g., `--api_key`). |
| `type` | String | Yes | The data type of the parameter (e.g., `string`, `float`, `boolean`). |
| `required` | Boolean | Yes | Specifies if this parameter is mandatory. |
| `ui_element` | String | Yes | The type of UI component to render for this parameter. See [UI Element Types](#ui-element-types) below. |
| `description`| String | Yes | A brief explanation of the parameter, which may be used for tooltips. |
| `default_value`| Any | Yes | A default value for the parameter. |
| `allowed_values`| Array/Object | No | A list of allowed values, used by `combobox` and `spinner` UI elements. |
| `step` | Number | No | The increment step for `spinner` UI elements. |

### UI Element Types

The `ui_element` field determines how the parameter is displayed in the settings panel:

| `ui_element` | Description | Additional Fields |
| :--- | :--- | :--- |
| `textfield` | A standard text input field. | - |
| `filePicker` | A text field with a "Browse" button to select files or directories. | - |
| `checkbox` | A checkbox for boolean (true/false) values. | - |
| `combobox` | A dropdown menu with a list of options. | `allowed_values`: An array of strings (e.g., `["model-a", "model-b"]`). |
| `spinner` | A number input with up/down arrows. | `allowed_values`: An object with `min` and `max` keys (e.g., `{"min": 0.0, "max": 1.0}`).<br>`step`: A number defining the increment value. |

### Example (`gemini_specifications.json`)

```json
[
  {
    "name": "Instruction Prompt Path",
    "argName": "--prompt_instruction_path",
    "type": "string",
    "required": true,
    "ui_element": "filePicker",
    "description": "Path to the predefined instruction to be applied to the user story",
    "default_value": ""
  },
  {
    "name": "API Key",
    "argName": "--api_key",
    "type": "string",
    "required": true,
    "ui_element": "textfield",
    "description": "Google Gemini API key",
    "default_value": ""
  },
  {
    "name": "Model",
    "argName": "--model",
    "type": "string",
    "required": true,
    "ui_element": "combobox",
    "description": "The model to use for generating completions",
    "allowed_values": [
      "gemini-1.5-flash",
      "gemini-1.5-pro"
    ],
    "default_value": "gemini-1.5-flash"
  }
]
```
</details>

---

<details>
<summary><h2>2. The Execution Script</h2></summary>

This script can be written in any language (Python, Bash, etc.) as long as it is executable from the command line.

### Purpose

The plugin will invoke this script, passing the values configured in the UI as command-line arguments. The script must then use these arguments to communicate with the LLM API and save the generated output.

### Required Command-Line Arguments

Your script **must** be able to accept the following arguments, in addition to the custom ones you define in your specification file:

| Argument | Description |
| :--- | :--- |
| `--user_story_path` | The absolute path to a temporary text file containing the user story to be processed. |
| `--output_dir_path` | The absolute path to the directory where the script **must** save its output. |

### Script Responsibilities

1.  **Parse Arguments**: The script must parse all incoming command-line arguments.
2.  **Read Input Files**: It needs to read the content of the files specified by `--prompt_instruction_path` (if defined in your spec) and `--user_story_path`.
3.  **Call the LLM API**: Using the parsed arguments (like API key, temperature, model, etc.), it should make a request to the LLM's API.
4.  **Save the Output**: The script **must** save the response from the LLM into a `.feature` file inside the directory specified by `--output_dir_path`. The filename can be anything, for instance `llm_output.feature`.

### Example (`gemini_main.py`)

This Python script demonstrates how to parse arguments, interact with an API, and save the output.

```python
import argparse
import os
import google.generativeai as genai

def main():
    parser = argparse.ArgumentParser(description="Parser for the Gemini API")

    # Arguments from the specification file
    parser.add_argument('--prompt_instruction_path', type=str, required=True)
    parser.add_argument('--api_key', type=str, required=True)
    parser.add_argument('--temperature', type=float, required=True)
    parser.add_argument('--model', type=str, required=True)
    
    # Required arguments from the plugin
    parser.add_argument('--user_story_path', type=str, required=True)
    parser.add_argument('--output_dir_path', type=str, required=True)

    args = parser.parse_args()

    # --- Script Logic ---
    # 1. Configure API
    genai.configure(api_key=args.api_key)
    model = genai.GenerativeModel(args.model)

    # 2. Read input files
    with open(args.prompt_instruction_path, 'r') as file:
        instruction = file.read()
    with open(args.user_story_path, 'r') as file:
        user_story = file.read()

    prompt = f"{instruction}\n\n{user_story}"

    # 3. Call LLM API
    response = model.generate_content(prompt)
    
    # 4. Save the output
    os.makedirs(args.output_dir_path, exist_ok=True)
    output_file = os.path.join(args.output_dir_path, "gemini_output.feature")
    with open(output_file, 'w') as file:
        file.write(response.text)

    print(f"Response saved at: {output_file}")

if __name__ == "__main__":
    main()
```
</details>

---

<details>
<summary><h2>How to Add a New LLM in IntelliJ IDEA</h2></summary>

Once you have created your specification `.json` file and your execution script, you can add them to the plugin.

### Step-by-Step Guide

1.  Navigate to **File > Settings > Tools > LLM Configuration**.
2.  Click on the dropdown menu at the top. It will show a list of existing LLM configurations.
3.  Select **"Insert new"** from the bottom of the list.
4.  A new set of fields will appear. Fill them out as follows:
    *   **LLM Name:** A unique, descriptive name for your LLM (e.g., "My Custom WizardLLM").
    *   **Select Script File:** Click "Browse" and locate your execution script (e.g., `wizard_main.sh`).
    *   **Select Configuration File:** Click "Browse" and locate your specification file (e.g., `wizard_spec.json`).
    *   **Console Command:** The command used to execute your script (e.g., `python3`, `bash`, `node`).
5.  After selecting the configuration file, the UI will dynamically render the parameters you defined in the JSON.
6.  Fill in any necessary default values for your new LLM's parameters.
7.  Click the **"Save"** button to add your new configuration.

Your custom LLM is now ready to be used by the BDDTestGen plugin!
</details>