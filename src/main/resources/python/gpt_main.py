import os
import time
import argparse
import sys

from openai import OpenAI

class BddAgent:
    def __init__(self, key):
        os.environ["OPENAI_API_KEY"] = key
        self.client = OpenAI()
        self.messages = []
        self.last_run = None
        self.last_response = None

    def append_message(self, role, content):
        self.messages.append({"role": role, "content": content})

    def append_message_from_file(self, role, file_path):
        self.append_message(role, Utils.file_to_string(file_path))

    def run(self, model="gpt-3.5-turbo-1106", save_response_to_message=True, temperature=None, seed=None):
        run_executed = False
        while not run_executed:
            try:
                params = {
                    "model": model,
                    "messages": self.messages,
                    "temperature": temperature
                }
                if seed is not None:
                    params["seed"] = seed

                self.last_run = self.client.chat.completions.create(**params)
                run_executed = True
            except Exception as e:
                time.sleep(30)
        self.last_response = self.last_run.choices[0].message
        if save_response_to_message:
            self.append_message(self.last_response.role, self.last_response.content)


class Utils:
    @classmethod
    def file_to_string(cls, file_name: str):
        with open(file_name, "r", encoding="utf-8") as input_file:
            return "".join(input_file.readlines())

    @classmethod
    def string_to_file(cls, file_name: str, string: str):
        with open(file_name, "w", encoding="utf-8") as output_file:
            output_file.write(string)

    @classmethod
    def strip_gherkin_formatting(cls, string: str):
        left_strip = "```gherkin"
        right_strip = "```"
        if string.startswith(left_strip):
            return string.lstrip(left_strip).rstrip(right_strip)
        return string


class Main:
    @staticmethod
    def add_initial_messages(agent: BddAgent, instruction_prompt_path: str, user_story_path: str):
        agent.append_message(role="user", content=Utils.file_to_string(instruction_prompt_path))
        agent.append_message_from_file(role="user", file_path=user_story_path)

    @staticmethod
    def run_like_chat(agent: BddAgent, model: str, prompt_instruction_path: str, user_story_path: str, temperature=None, seed=None):
        Main.add_initial_messages(agent, prompt_instruction_path, user_story_path)
        while True:
            try:
                agent.run(model, temperature=temperature, seed=seed)
                response_content = agent.last_response.content
                response_content = Utils.strip_gherkin_formatting(response_content)
                return response_content
            except Exception as e:
                continue

    @staticmethod
    def run(prompt_instruction_path, user_story_path, key_string, output_dir_path, times_to_run, temperature, seed, model, debug=False):
        if times_to_run == 0:
            raise Exception("Error: trying to run 0 times")
        starting_run = 1
        final_run = times_to_run + 1
        for i in range(starting_run, final_run + 1):
            agent = BddAgent(key_string)
            result = Main.run_like_chat(agent=agent, model=model, prompt_instruction_path=prompt_instruction_path,
                                        user_story_path=user_story_path, temperature=temperature, seed=seed)
            if debug:
                for c, message in enumerate(agent.messages):
                    content = message["content"]
                    output_path = os.path.join(output_dir_path, f'msg{c}.txt')
                    with open(output_path, 'w', encoding="utf-8") as file:
                        file.write(content)
            return result


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description='Parser for the arguments passed in Kotlin environment')

    parser.add_argument('--prompt_instruction_path', type=str, required=True,
                        help='Path to the predefined instruction to be applied to the user story')
    parser.add_argument('--user_story_path', type=str, required=True, help='Path to the user story')
    parser.add_argument('--api_key', type=str, required=True, help='OpenAI API key')
    parser.add_argument('--output_dir_path', type=str, required=True, help='Path to the output directory')
    parser.add_argument('--temperature', type=float, required=True, help='Temperature for the model')
    parser.add_argument('--seed', type=int, default=None, nargs='?', help='Seed for reproducibility (optional)')
    parser.add_argument('--debug', action='store_true', help='Whether to run the script in debug mode')
    parser.add_argument('--model', type=str, required=True,
                        help='The model to use for generating completions')

    args = parser.parse_args()

    print(f"Prompt Path: {args.prompt_instruction_path}\n")

    try:
        result = Main.run(
            prompt_instruction_path=args.prompt_instruction_path,
            user_story_path=args.user_story_path,
            key_string=args.api_key,
            output_dir_path=args.output_dir_path,
            times_to_run=1,
            temperature=args.temperature,
            seed=args.seed,
            model=args.model,
            debug=args.debug
        )

        print(result)  # Print the result to stdout
        output_path = os.path.join(args.output_dir_path, 'gpt_output.feature')

        with open(output_path, 'w', encoding="utf-8") as file:
            file.write(result)

        sys.exit(0)  # Success
    except Exception as e:
        print(f"Error: {str(e)}", file=sys.stderr)
        sys.exit(1)  # Failure
