import argparse
import os
import requests
import json

DEEPSEEK_API_URL = "https://api.deepseek.com/v1/chat/completions"  # Endpoint correto

def load_file_content(file_path):
    """Lê o conteúdo de um arquivo."""
    if not os.path.exists(file_path):
        raise FileNotFoundError(f"Arquivo não encontrado: {file_path}")

    with open(file_path, "r", encoding="utf-8") as file:
        return file.read().strip()

def call_deepseek_api(api_key, model, prompt, user_story, temperature):
    """Faz uma requisição à API do DeepSeek."""
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
        "max_tokens": 4096  # Define um limite para a resposta
    }

    response = requests.post(DEEPSEEK_API_URL, headers=headers, json=payload)

    if response.status_code == 200:
        return response.json()["choices"][0]["message"]["content"]
    else:
        raise Exception(f"Erro na API DeepSeek: {response.status_code} - {response.text}")

def main():
    parser = argparse.ArgumentParser(description="Executa a API do DeepSeek com um prompt e uma história de usuário.")
    parser.add_argument("--prompt_instruction_path", type=str, required=True, help="Caminho do arquivo de instrução do prompt")
    parser.add_argument("--user_story_path", type=str, required=True, help="Caminho do arquivo de história do usuário")
    parser.add_argument("--api_key", type=str, required=True, help="Chave da API do DeepSeek")
    parser.add_argument("--output_dir_path", type=str, required=True, help="Diretório de saída para salvar a resposta")
    parser.add_argument("--temperature", type=float, required=True, help="Temperatura do modelo")
    parser.add_argument("--model", type=str, required=True, choices=["deepseek-chat", "deepseek-coder"], help="Modelo a ser usado (deepseek-chat ou deepseek-coder)")
    parser.add_argument("--debug", action="store_true", help="Ativa o modo debug")

    args = parser.parse_args()

    try:
        # Lendo os arquivos
        prompt = load_file_content(args.prompt_instruction_path)
        user_story = load_file_content(args.user_story_path)

        if args.debug:
            print(f"📌 PROMPT:\n{prompt}\n")
            print(f"📌 HISTÓRIA DO USUÁRIO:\n{user_story}\n")
            print(f"📌 Modelo: {args.model}, Temperatura: {args.temperature}")

        # Chamando a API
        response = call_deepseek_api(args.api_key, args.model, prompt, user_story, args.temperature)

        # Salvando o resultado no diretório de saída
        os.makedirs(args.output_dir_path, exist_ok=True)
        output_file = os.path.join(args.output_dir_path, "deepseek_response.txt")

        with open(output_file, "w", encoding="utf-8") as file:
            file.write(response)

        print(f"✅ Resposta salva em: {output_file}")

    except Exception as e:
        print(f"❌ Erro: {e}")

if __name__ == "__main__":
    main()
