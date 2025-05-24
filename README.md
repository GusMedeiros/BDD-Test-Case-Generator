# BDD TestGen Plugin

<!-- Plugin description -->
BDD TestGen is an IntelliJ IDEA plugin designed to facilitate the process of generating BDD (Behavior-Driven Development) artifacts from user stories. By integrating with powerful Large Language Models (LLMs), this plugin helps you create comprehensive test scenarios directly within your IDE.
<!-- Plugin description end -->

This document provides instructions for manual installation, configuration, and usage of the BDD TestGen plugin.

## Prerequisites

Before you begin, ensure you have the following installed:

*   **IntelliJ IDEA:** Version 2025.2 or later.
*   **Python:** A recent version of Python must be installed and accessible from your system's PATH.

## Installation

To manually install the BDD TestGen plugin, follow these steps:

1.  **Download the Plugin:**
    Navigate to the "Releases" section of this repository and download the latest `.zip` file of the plugin.

2.  **Install from Disk:**
    *   Open IntelliJ IDEA.
    *   Go to `File` -> `Settings` (or `IntelliJ IDEA` -> `Preferences` on macOS).
    *   Select the "Plugins" tab.
    *   Click on the gear icon (⚙️) and choose "Install Plugin from Disk...".
    *   In the file browser, locate and select the downloaded `.zip` file.
    *   Click "OK" and restart IntelliJ IDEA when prompted.

## Dependencies

The plugin comes with three default LLM scripts. To use a specific LLM, you must install its corresponding Python library if you haven't already.

Note: Dependency versions below are confirmed to work but future versions are likely to work.
*   **For ChatGPT:**
    ```bash
    pip install openai==1.106.1
    ```

*   **For Gemini:**
    ```bash
    pip install google-generativeai==0.8.5
    ```

*   **For DeepSeek:**
    ```bash
    pip install requests==2.32.5
    ```

## Configuration

Before using the plugin, you need to configure your desired LLM profile:

1.  **Access Settings:**
    In the IntelliJ IDEA menu, go to `Tools` -> `Change BDDTestGen Settings`.

2.  **Enter Your Settings and Credentials:**
    A dialog box will appear. Select the LLM you wish to use from the available profiles. The required fields will vary depending on the selected LLM. Fill in the necessary information (e.g., API keys, model names).
    Pay special attention to the Console Command field. For windows it's usually "python", for linux it's usually python3.
3.  **Save and Select:**
    Click "OK" to save your settings. The selected LLM profile is now active and future runs will use it.

## Usage

To generate BDD test cases from a user story:

1.  **Locate User Story:**
    In the Project view, find the file containing the user story you want to generate test cases for.

2.  **Run BDDTestGen:**
    Right-click on the user story file.

3.  **Generate Test cases:**
    From the context menu, select "Run BDDTestGen". The plugin will then process the user story using your configured LLM and generate the corresponding BDD tests.