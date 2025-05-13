import argparse
import os
import requests
import json

DEEPSEEK_API_URL = "https://api.deepseek.com/v1/chat/completions"  # Correct endpoint

def load_file_content(file_path):
    """Reads the content of a file."""
    if not os.path.exists(file_path):
        raise FileNotFoundError(f"File not found: {file_path}")

    with open(file_path, "r", encoding="utf-8") as file:
        return file.read().strip()

def call_deepseek_api(api_key, model, prompt, user_story, temperature):
    """Makes a request to the DeepSeek API."""
    headers = {
        "Authorization": f"Bearer {api_key}",
        "Content-Type": "application/json"
    }

    payload = {
        "model": model,
        "messages": [
            {"role": "system", "content": prompt},
            {"role": "user", "content": user_story}
        ],
        "temperature": temperature,
        "max_tokens": 4096  # Sets a limit for the response
    }

    response = requests.post(DEEPSEEK_API_URL, headers=headers, json=payload)

    if response.status_code == 200:
        return response.json()["choices"][0]["message"]["content"]
    else:
        raise Exception(f"DeepSeek API error: {response.status_code} - {response.text}")

def main():
    parser = argparse.ArgumentParser(description="Calls the DeepSeek API using a prompt and a user story.")
    parser.add_argument("--prompt_instruction_path", type=str, required=True, help="Path to the prompt instruction file")
    parser.add_argument("--user_story_path", type=str, required=True, help="Path to the user story file")
    parser.add_argument("--api_key", type=str, required=True, help="DeepSeek API key")
    parser.add_argument("--output_dir_path", type=str, required=True, help="Output directory to save the response")
    parser.add_argument("--temperature", type=float, required=True, help="Model temperature")
    parser.add_argument("--model", type=str, required=True, choices=["deepseek-chat", "deepseek-coder"], help="Model to use (deepseek-chat or deepseek-coder)")
    parser.add_argument("--debug", action="store_true", help="Enable debug mode")

    args = parser.parse_args()

    try:
        # Reading the files
        prompt = load_file_content(args.prompt_instruction_path)
        user_story = load_file_content(args.user_story_path)

        if args.debug:
            print(f"📌 PROMPT:\n{prompt}\n")
            print(f"📌 USER STORY:\n{user_story}\n")
            print(f"📌 Model: {args.model}, Temperature: {args.temperature}")

        # Calling the API
        response = call_deepseek_api(args.api_key, args.model, prompt, user_story, args.temperature)

        # Saving the result in the output directory
        os.makedirs(args.output_dir_path, exist_ok=True)
        output_file = os.path.join(args.output_dir_path, "deepseek_response.txt")

        with open(output_file, "w", encoding="utf-8") as file:
            file.write(response)

        print(f"✅ Response saved at: {output_file}")

    except Exception as e:
        print(f"❌ Error: {e}")

if __name__ == "__main__":
    main()
