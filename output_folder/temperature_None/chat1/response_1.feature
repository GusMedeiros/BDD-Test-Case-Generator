Funcionalidade:  Cadastro de Local

Cenário 1: Realizar cadastro de um novo local válido
    Dado que estou na tela 'Cadastrar Local'
    Quando preencho o campo 'CEP do local' com <cep>
    E preencho o campo 'Número' com <numero>
    E pressiono o botão 'Buscar'
    E preencho os campos restantes com as informações recebidas
    E pressiono o botão 'Salvar'
    Então um novo local é criado com as informações fornecidas
    E um mapa é exibido com um marcador indicando a geolocalização do local cadastrado

    Exemplos:
        | cep | numero |
        | "01010000" | "50" |
        | "04811100" | "123" |
        | "02052020" | "30" |
        | "01010101" | "9" |
        | "99999999" | "100" |

Cenário 2: Tentativa de realizar cadastro de um novo local com CEP inválido
    Dado que estou na tela 'Cadastrar Local'
    Quando preencho o campo 'CEP do local' com <cep>
    E preencho o campo 'Número' com <numero>
    E pressiono o botão 'Buscar'
    Então um erro é exibido informando que o CEP fornecido é inválido

    Exemplos:
        | cep | numero |
        | "123" | "50" |
        | "abcdefgh" | "123" |
        | "" | "9" |

Cenário 3: Tentativa de realizar cadastro de um novo local sem coordenadas
    Dado que estou na tela 'Cadastrar Local'
    Quando preencho o campo 'CEP do local' com <cep>
    E preencho o campo 'Número' com <numero>
    E pressiono o botão 'Salvar'
    Então um erro é exibido informando que é necessário preencher as coordenadas do local

    Exemplos:
        | cep | numero |
        | "01010000" | "50" |
        | "04811100" | "123" |