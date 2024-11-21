import argparse
import os
import google.generativeai as genai


def main():
    # Configurando o parser de argumentos
    parser = argparse.ArgumentParser(description="Parser for the arguments passed to the Gemini API script")
    parser.add_argument('prompt_instruction_path', type=str,
                        help='Path to the predefined instruction to be applied to the user story')
    parser.add_argument('user_story_path', type=str, help='Path to the user story')
    parser.add_argument('api_key', type=str, help='Google Gemini API key')
    parser.add_argument('output_dir_path', type=str, help='Path to the output directory')
    parser.add_argument('temperature', type=float, help='Temperature for the model')
    parser.add_argument('seed', type=int, nargs='?', default=None, help='Seed for reproducibility')
    parser.add_argument('debug', type=str, help='Whether to run the script in debug mode', default='False')
    parser.add_argument('model', type=str, help='The model to use for generating completions')

    args = parser.parse_args()

    # Configurando a chave de API
    os.environ["GEMINI_API_KEY"] = args.api_key
    genai.configure(api_key=args.api_key)
    model = genai.GenerativeModel(args.model)

    # Lendo os arquivos de instrução e história do usuário
    with open(args.prompt_instruction_path, 'r', encoding='utf-8') as file:
        instruction = file.read()
    with open(args.user_story_path, 'r', encoding='utf-8') as file:
        user_story = file.read()

    # Criando o prompt completo
    prompt = f"{instruction}\n\n{user_story}"

    # Configurações de geração
    generation_config = genai.types.GenerationConfig(
        temperature=args.temperature,
    )

    if args.debug.lower() == 'true':
        print(f"Prompt enviado à API:\n{prompt}")
        print(f"Configurações de geração: {generation_config}")

    # Gerando a resposta da LLM
    try:
        response = model.generate_content(prompt, generation_config= generation_config)
        result = response.text
    except Exception as e:
        print(f"Erro ao gerar resposta: {e}")
        return

    # Salvando a resposta no arquivo de saída
    output_file = os.path.join(args.output_dir_path, "response.txt")
    os.makedirs(args.output_dir_path, exist_ok=True)
    with open(output_file, 'w', encoding='utf-8') as file:
        file.write(result)

    print(f"Resposta salva em: {output_file}")


if __name__ == "__main__":
    main()
