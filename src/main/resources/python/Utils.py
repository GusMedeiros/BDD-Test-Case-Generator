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
