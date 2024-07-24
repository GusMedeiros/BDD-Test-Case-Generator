from openai import OpenAI

client = OpenAI()

models = client.models.list()

# Filtrar apenas modelos GPT e extrair os IDs
gpt_model_ids = [model.id for model in models.data if 'gpt' in model.id]

for model in gpt_model_ids:
    print(model)
