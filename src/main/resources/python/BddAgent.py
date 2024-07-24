import os
import time

from openai import OpenAI

from Utils import Utils


class BddAgent:

    def __init__(self, key):
        os.environ["OPENAI_API_KEY"] = key
        self.client = OpenAI()
        self.messages = []
        self.last_run = None
        self.last_response = None

    def add_message(self, role, content):
        self.messages.insert(0, {"role": role, "content": content})

    def append_message(self, role, content):
        self.messages.append({"role": role, "content": content})

    def append_message_from_file(self, role, file_path):
        self.append_message(role, Utils.file_to_string(file_path))

    def add_message_from_file(self, role, file_path):
        self.add_message(role, Utils.file_to_string(file_path))

    def run(self, model="gpt-3.5-turbo-1106", save_response_to_message=True, temperature=None, seed=None):
        run_executed = False
        while not run_executed:
            try:
                if seed is None:
                    self.last_run = self.client.chat.completions.create(
                        model=model,
                        messages=self.messages,
                    )
                else:
                    self.last_run = self.client.chat.completions.create(
                        model=model,
                        messages=self.messages,
                        temperature=temperature,
                        seed=seed
                    )
                run_executed = True
            except Exception as e:
                time.sleep(30)
        self.last_response = self.last_run.choices[0].message
        if save_response_to_message:
            self.add_message(self.last_response.role, self.last_response.content)