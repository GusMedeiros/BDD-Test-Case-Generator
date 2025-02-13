import argparse
import os
import google.generativeai as genai

def main():
    # Configurando o parser de argumentos com parâmetros nomeados
    parser = argparse.ArgumentParser(description="Parser for the Gemini API script with named arguments")

    parser.add_argument('--prompt_instruction_path', type=str, required=True,
                        help='Path to the predefined instruction to be applied to the user story')
    parser.add_argument('--user_story_path', type=str, required=True, help='Path to the user story')
    parser.add_argument('--api_key', type=str, required=True, help='Google Gemini API key')
    parser.add_argument('--output_dir_path', type=str, required=True, help='Path to the output directory')
    parser.add_argument('--temperature', type=float, required=True, help='Temperature for the model')
    parser.add_argument('--seed', type=int, default=None, help='Seed for reproducibility')
    parser.add_argument('--debug', action='store_true', help='Enable debug mode')
    parser.add_argument('--model', type=str, required=True,
                        choices=["gemini-1", "gemini-1.5", "gemini-pro", "gemini-ultra"],
                        help='The model to use for generating completions')

    args = parser.parse_args()

    # Configurando a chave de API
    os.environ["GEMINI_API_KEY"] = args.api_key
    genai.configure(api_key=args.api_key)
    model = genai.GenerativeModel(args.model)

    # Lendo os arquivos de instrução e história do usuário
    try:
        with open(args.prompt_instruction_path, 'r', encoding='utf-8') as file:
            instruction = file.read()
        with open(args.user_story_path, 'r', encoding='utf-8') as file:
            user_story = file.read()
    except FileNotFoundError as e:
        print(f"Erro: Arquivo não encontrado -> {e}")
        return
    except Exception as e:
        print(f"Erro ao ler arquivos -> {e}")
        return

    # Criando o prompt completo
    prompt = f"{instruction}\n\n{user_story}"

    # Configurações de geração
    generation_config = genai.types.GenerationConfig(
        temperature=args.temperature,
        seed=args.seed
    )

    if args.debug:
        print(f"Prompt enviado à API:\n{prompt}")
        print(f"Configurações de geração: {generation_config}")

    # Gerando a resposta da LLM
    try:
        response = model.generate_content(prompt, generation_config=generation_config)
        result = response.text
    except Exception as e:
        print(f"Erro ao gerar resposta: {e}")
        return

    # Criando o diretório de saída, se necessário
    os.makedirs(args.output_dir_path, exist_ok=True)

    # Salvando a resposta no arquivo de saída
    output_file = os.path.join(args.output_dir_path, "gemini_output.feature")
    with open(output_file, 'w', encoding='utf-8') as file:
        file.write(result)

    print(f"Resposta salva em: {output_file}")


if __name__ == "__main__":
    main()
