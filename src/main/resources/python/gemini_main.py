import argparse
import os
import google.generativeai as genai

os.environ["GRPC_VERBOSITY"] = "ERROR"
os.environ["GLOG_minloglevel"] = "2"

def main():
    parser = argparse.ArgumentParser(description="Parser for the Gemini API script with named arguments")

    parser.add_argument('--prompt_instruction_path', type=str, required=True,
                        help='Path to the predefined instruction to be applied to the user story')
    parser.add_argument('--user_story_path', type=str, required=True, help='Path to the user story')
    parser.add_argument('--api_key', type=str, required=True, help='Google Gemini API key')
    parser.add_argument('--output_dir_path', type=str, required=True, help='Path to the output directory')
    parser.add_argument('--temperature', type=float, required=True, help='Temperature for the model')
    parser.add_argument('--debug', action='store_true', help='Enable debug mode')
    parser.add_argument('--model', type=str, required=True,
                        help='The model to use for generating completions')

    args = parser.parse_args()

    os.environ["GEMINI_API_KEY"] = args.api_key
    genai.configure(api_key=args.api_key)

    model = genai.GenerativeModel(args.model)

    try:
        with open(args.prompt_instruction_path, 'r', encoding='utf-8') as file:
            instruction = file.read()
        with open(args.user_story_path, 'r', encoding='utf-8') as file:
            user_story = file.read()
    except FileNotFoundError as e:
        print(f"Error: File not found -> {e}")
        return
    except Exception as e:
        print(f"Error reading files -> {e}")
        return

    prompt = f"{instruction}\n\n{user_story}"

    generation_config = genai.GenerationConfig(
        temperature=args.temperature
        # 'seed' parameter is removed, no longer supported in Gemini models.
    )

    if args.debug:
        print(f"Prompt sent to the API:\n{prompt}")
        print(f"Generation configuration: {generation_config}")

    try:
        response = model.generate_content(prompt, generation_config=generation_config)
        result = response.text
    except Exception as e:
        print(f"Error generating response: {e}")
        return

    os.makedirs(args.output_dir_path, exist_ok=True)

    output_file = os.path.join(args.output_dir_path, "gemini_output.feature")
    with open(output_file, 'w', encoding='utf-8') as file:
        file.write(result)

    print(f"Response saved at: {output_file}")


if __name__ == "__main__":
    main()
