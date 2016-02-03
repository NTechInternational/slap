from string import Template
import re


class Transformer:

    @staticmethod
    def convert_answers_to_proper_format(questions):
        """
        Transform answer in question from
        "answers": "ChoiceText:Big ticket (realtor or car dealer)| ChoiceFacet:BusinessModel, ...
        to
        "answers" : [
            { "ChoiceText" : "Big ticket (realtor or car delear)" },
            { "ChoiceFacet" : "BusinessModel" },
            ...
        ]
        """
        for question in questions:
            answers = question.get('answers')
            formatted_answers = []
            alloptions = answers.split('|') #split options separated by pipe
            for option in alloptions:
                key_values = option.split(',')
                answer = {}
                for key_value in key_values:
                    key_value_pair = key_value.split(':', 1)
                    if len(key_value_pair) == 2:
                        answer[key_value_pair[0].strip()] = key_value_pair[1].strip()

                formatted_answers.append(answer)

            if len(formatted_answers) > 0:
                question['answers'] = formatted_answers

    @staticmethod
    def convert_items_to_proper_format(items):
        """
        Transform itemtemplate in items/challenges from
        "itemtemplate" : [Beginning &StartTime and ending] [by &Deadline] [be one of the first &RewardLimit people to] become a new &Customer [when you &OfferAction &OfferingAmount &TeaserOffering] [and &Activity,] and we will provide a coupon [for &RewardAmount &Reward] [to &Beneficiary].
        """
        ItemTransformer(items = items).transform()


class VariableTemplate(Template):
    """
    This class helps identify substitute the values of variables in the item template
    """

    delimiter = '&'
    idpattern = r'[\w\d]+'



class ItemTransformer:

    ITEM_TEMPLATE_KEY = 'itemtemplate'
    VARIABLE_KEY = 'variables'

    def __init__(self, items):
        self.items = items

    def transform(self, answered_variables = None):
        """
        Transforms individual item template based on default and answered variables

        :param answered_variables: the list of variables for which an answer has been provided.
        :return:
        """
        if answered_variables is None:
            answered_variables = {}

        for item in self.items:
            variables = self.__get_defaults(item[self.VARIABLE_KEY])
            variables.update(answered_variables)
            item[self.VARIABLE_KEY] = variables
            self.__fill_values(item, variables, answered_variables)
            self.__correct_grammar(item)

        return {}

    def __fill_values(self, item, variables, answered_variables):
        """
        Each item template consists of
        variables text prefixed by '&'
        and optional blocks enclosed in '[' ']'
        E.g.: [Beginning &StartTime and ending] [by &Deadline] &OfferAction &OfferingAmount &Offering [and &Activity,] and &Reward] [to &Beneficiary].
        So in a more generic terms
        A template consists of
        <template> ::= <word>|<optional statement>|<variable>|<punctuation>
        <variable> ::= &<word>
        <optional statement> = [<word>|<variable>|<punctuation>
        <punctuation> :== <dot>|<comma>
        """
        # discover all variables
        substitution_summary = []
        variables_with_defaults = []
        variables_without_values = []

        item_template = item[self.ITEM_TEMPLATE_KEY]

        VARIABLE_REGEX = '&(\w+)'
        matches = []

        for match in re.finditer(VARIABLE_REGEX, item_template):
            matches.append(match)

        matches.reverse()

        for match in matches:
            variable = item_template[match.start(1):match.end(1)]
            if variable in variables:
                substituting_value = variables[variable]
                item_template = item_template[:match.start()] + substituting_value + item_template[match.end():]
                substitution_summary.append({'variable' : '&' + variable, 'value' : substituting_value })
                if variable not in answered_variables:
                    variables_with_defaults.append(variable)
            else:
                variables_without_values.append(variable)

        # item[self.ITEM_TEMPLATE_KEY] = VariableTemplate(item_template).safe_substitute(answered_variables)

        item['defaultsOnly'] = variables_with_defaults
        item['missingVariables'] = variables_without_values
        item['substitutionSummary'] = substitution_summary
        item[self.ITEM_TEMPLATE_KEY] = item_template

        # remove any variables in optional string
        self.__remove_optionals(item)

    def __get_defaults(self, default_variables):
        """
        This method extracts the default variable values provided from Solr
        &Customer:customer (Become a new customer and we will provide a coupon.)", "Facets_s": "BusinessModel:All, Goal:NewCustomer, RewardType:Coupon

        As seen the variable is a comma separated and is name:value pair
        """
        variables = {}
        for variable in default_variables.split(','):
            key_value = variable.split(':')
            variables[key_value[0].strip(' &')] = key_value[1].strip()

        return variables

    def __remove_optionals(self, item):
        """
        removes any optional section without complete variable information
        :param item: the item whose optional segments are to be removed
        :return:
        """
        OPTIONAL_SECTION_REGEX = '\[(.*?)\]'
        VARIABLE_REGEX = '&\w+'

        # Find optional segments contained within square braces example: [Beginning &StartTime and ending]
        match = re.search(OPTIONAL_SECTION_REGEX, item[self.ITEM_TEMPLATE_KEY])
        variables_without_value = []

        while match != None:
            # for each match of optional section check if the optional section has any variables that haven't been
            # substituted. We will remove such sections.
            tmpl = item[self.ITEM_TEMPLATE_KEY]

            if re.search(VARIABLE_REGEX, match.group()) != None:
                item[self.ITEM_TEMPLATE_KEY] = tmpl[:match.start()] + tmpl[match.end():]
            else:
                item[self.ITEM_TEMPLATE_KEY] = tmpl[:match.start()] + match.group(1) + tmpl[match.end():]

            match = re.search(OPTIONAL_SECTION_REGEX, item[self.ITEM_TEMPLATE_KEY])

    def __correct_grammar(self, item):
        tmpl = item[self.ITEM_TEMPLATE_KEY]

        # remove any extra spaces between words
        tmpl = re.sub('\s{2,}', ' ', tmpl)

        # replace any period followed by a period into single period
        tmpl = re.sub('(\.(\s|\.)*\.)', '.', tmpl)

        # remove any trailing and preceding white spaces
        tmpl = tmpl.strip()

        # Change the case of sentence to upper case
        tmpl = tmpl[0].upper() + tmpl[1:]

        for match in re.finditer(r'[\.!\?]\s*(\w)', tmpl):
            tmpl = tmpl[:match.start(1)] + match.group(1).upper() + tmpl[match.end(1):]

        # Remove any square brackets that are still in the template
        tmpl = re.sub('\[|\]', '', tmpl)


        item[self.ITEM_TEMPLATE_KEY] = tmpl


