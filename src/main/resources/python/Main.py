import os
import time

from openai import OpenAI



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
        print(f"prompt: \n {Utils.file_to_string(file_path)}")

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
        if string[0:10] == left_strip:
            return string.lstrip(left_strip).rstrip(right_strip)
        return string


import argparse
import sys

prompt = """
Atue como um programador trabalhando com Behavior Driven Development (BDD). No BDD, você começa com a definição de 'Funcionalidade', que descreve o comportamento desejado do software; em seguida, você elabora o 'Cenário', detalhando um caso de uso específico dessa 'Funcionalidade'; e, por fim, são especificados os 'Exemplos', que são dados específicos utilizados para ilustrar um ‘Cenário’.

No próximo prompt eu irei te enviar uma história de usuário como entrada e, com base nela, gerar como saída apenas um único arquivo .feature que contém um conjunto de 'Exemplos' logo abaixo de cada 'Cenário'.  Você deverá criar  'Exemplos ' com um conjunto de valores de forma a atender os critérios de 'Particionamento em Classes de Equivalência' e 'Análise do Valor Limite' .

Crie a tabela de 'Exemplos' abaixo de cada 'Cenário' correspondente, onde cada título de coluna corresponde às variáveis definidas no 'Cenário'. Ou seja, se forem gerados 'n' cenários, então o arquivo .feature também deverá gerar 'n' tabelas de 'Exemplos' seguido de seu respectivo 'Cenário'. Certifique-se de que o arquivo .feature siga a sintaxe da linguagem Gherkin corretamente.

Ao elaborar o arquivo, mantenha a resposta no mesmo idioma da entrada recebida, incluindo as palavras-chaves da sintaxe Gherkin. Certifique-se de respeitar todos os nomes de variáveis citados na história do usuário. Coloque as variáveis do 'Cenário' entre sinais de menor que (<) e maior que (>). Lembre-se também que as palabras seguidas de um '*' no final nas história de usuário, significa que são campos obrigatórios. Não converse com o usuário, não faça anotações ou observações, apenas retorne o arquivo pedido.

Segue um exemplo para você entender a estrutura de como queremos que seja a saída.

“””
Cenário: Adicionar números
     Dado o <num1>  inserido na calculadora
     E  digitar <num2> na calculadora
     Quando pressiono o botão Add
     Então o resultado deve ser <resultado> na tela

     Exemplos:
       | num1 | num2 | resultado |
       | 1 | 2 | 3 |
       | 2 | 3 | 5 |
       | -5 | -5 | -10 |
       | -5 | 10 | 5 |
       | 5 | -10 | -5 |
       | 0.3 | 0.3 | 0.6 |
       | 0 | 0 | 0 |
       | -5 | 5 | 0 |

Cenário: Adicionar números inválidos
     Dado o <num1>  inserido na calculadora
     E  digitar <num2> na calculadora
     Quando pressiono o botão Add
     Então o resultado deve ser <resultado> na tela

     Exemplos:
       | num1 | num2 | resultado |
       | a | b | error |
       | “ ” | b | error |
       |a |  “ ” | error |
 “””

História de usuario:

Eu, como administrador do sistema, desejo cadastrar um local.
Títulos das telas: Dashboard, Lista de Locais, Cadastrar Local.
O usuário navega na tela de Dashboard para o Menu lateral, pressiona o botão Locais, contendo uma lista de entradas correspondentes, cada um a um local, indicando:
⦁ Descrição do local*
⦁ Endereço do local*
⦁ Data de criação do local*
⦁ Data da última edição do local*
⦁ Opções de ação*

Na parte superior da lista, há um campo de busca e um botão Novo Local. Ao clicar em Novo Local, é exibida a página Cadastrar Local, onde é possível cadastrar um novo local com as seguintes informações:
⦁ Descrição do local*
⦁ CEP do local*
⦁ Endereço do local*
⦁ Número*
⦁ Complemento
⦁ Bairro*
⦁ Município*
⦁ Unidade Federativa*
⦁ País*
⦁ Latitude*
⦁ Longitude*

Ao preencher a informação CEP, as outras informações são preenchidas automaticamente, com exceção das informações não obrigatórias Complemento, Latitude e Longitude.

Ao lado do campo de longitude, deve haver um botão “Buscar” que deve permitir ao usuário realizar uma busca de geolocalização do endereço fornecido para recuperar as coordenadas a partir de um algoritmo de geocoding.

Abaixo do formulário, deve ser exibido um mapa com um marcador da geolocalização das coordenadas preenchidas.

Regras de Negócio:
RN01: A informação CEP deve ser um valor numérico válido de 8 dígitos.
RN02: O mapa só deve ser exibido caso haja alguma coordenada preenchida.
"""

class Main:

    @staticmethod
    def add_initial_messages(agent: BddAgent, instruction_prompt_path: str, user_story_path: str):
        agent.add_message(role="user", content= prompt)
        agent.add_message_from_file(role="user", file_path=user_story_path)

    @staticmethod
    def run_like_chat(agent: BddAgent, model: str, prompt_instruction_path: str, user_story_path: str):
        Main.add_initial_messages(agent, prompt_instruction_path, user_story_path)
        while True:
            try:
                agent.run(model)
                response_content = agent.last_response.content
                response_content = Utils.strip_gherkin_formatting(response_content)
                return response_content
            except Exception as e:
                continue

    @staticmethod
    def run(prompt_instruction_path, user_story_path, key_string, times_to_run):
        if times_to_run == 0:
            raise Exception("Error: trying to run 0 times")
        starting_run = 1
        final_run = times_to_run + 1
        for i in range(starting_run, final_run + 1):
            agent = BddAgent(key_string)
            result = Main.run_like_chat(agent=agent, model="gpt-4o", prompt_instruction_path=prompt_instruction_path,
                                      user_story_path=user_story_path)

            for c, message in enumerate(agent.messages):
                content = message["content"]
                user_story_dir = os.path.dirname(args.user_story_path)
                caminho_output = os.path.join(user_story_dir, f'msg{c}.txt')
                with open(caminho_output, 'w', encoding="utf-8") as arquivo:
                    arquivo.write(content)
            return result


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description='Parser for the arguments passed in kotlin environment')
    parser.add_argument('prompt_instruction_path', type=str,
                        help='Path to the predefined instruction to be applied to the user story')
    parser.add_argument('user_story_path', type=str, help='Path to the user story')
    parser.add_argument('api_key', type=str, help='Openai api key')
    args = parser.parse_args()

    print(f"prompt path: {args.prompt_instruction_path}\n")

    try:
        result = Main.run(args.prompt_instruction_path, args.user_story_path, args.api_key, 1)
        print(result)  # Print the result to stdout
        user_story_dir = os.path.dirname(args.user_story_path)
        caminho_output = os.path.join(user_story_dir, 'BDD_output.feature')
        with open(caminho_output, 'w', encoding="utf-8") as arquivo:
            arquivo.write(result)
        sys.exit(0)  # Success
    except Exception as e:
        print(f"Error: {str(e)}", file=sys.stderr)
        sys.exit(1)  # Failure
