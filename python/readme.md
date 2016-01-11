# SLAP Documentation

#### API Description

* /rest/getvisitorid?userid={User ID} : This api registers a given user id and returns a valid _visitorId_, if the user id has already been registered it returns the existing _visitorId_ for the user

* /rest/processrequest?visitorId={Visitor Id}&type={interaction type} : This api performs various actions based on the interaction type. Additional params supported for interactions are listed below.
  * Select interaction:
    * itemid: the challenge that is being selected
  * Submit interaction:
    * var: {Variable Name}:{Variable value}

      Multiple occurrence of _var_ parameter is supported. Each var parameter should provide the variable and the value of it.

    * facet: {Facet Name}:{Facet value}

      Multiple occurrence of _facet_ parameter is supported. Each facet parameter should provide the facet and the value of it.

#### Description of various interactions:

* ##### No transaction type in the URL:
    * __Challenge has been selected previously__: This works similarly to __Select__ interaction.
    * __Facets have been selected previously__: Filter based on the facets and return a row of question and challenge
    * __Else__:

* ##### Reset Transaction Type:
    * Save all the submitted question to the incomplete session history
    * Remove all interaction log (used by back interaction)
    * Clear selected challenge
    * Clear previously submitted questions
    * Perform __default__ interaction

* ##### Done Transaction Type:
    * Save all the submitted question to complete session history
    * Save final text (Get Param: text) along with history
    * Remove all interaction log
    * Clear previously submitted questions
    * Perform __default__ interaction

* ##### Submit Transaction Type:
    * get all facets and variables submission for the question
    * save the variable and question

* ##### Select Transaction Type:
    * save the selected transaction in selectedChallenge
    * query the database to get the missing variables in the challenge (item)
    * query the database for question with the missing variables
    * query the database for question with default variables
    * return response 

* ##### Back Transaction Type:
    * get the interaction log for the visitor
    * pop the last interaction
    * if select interaction, overwrite the selected challenge id
    * else if submit interaction delete the submitted challenge and activate the overwritten submission, unset the overwritten flag.
    * update the interaction log.


#### Mongo DB Collections and data:

* __visitors__: This collection stores information about the visitor id and selected challenge.

```
      visitorId: Id of the visitor (randomly generated by getVisitor api)
      userId: User Id of the visitor
      selectedChallenge: the challenge that has been previously selected by the user
```

* __questions__: This collection stores information about the question submission that have been done by a user

```
      visitorId: Id of the visitor submitting the question response
      questionId: Id of the question related to the submission
      variables: list of submissions of variables stored as 
                 {variableName : value}, note variableName has '&' prepended
      facets: list of facets selected stored as
                 {facetName : value}
      isOverwritten : boolean indicating whether the question is valid or not, used when restoring the back.
```

* __visitorSession__: This collection stores information about the user's submission for the session

```
      visitorId: Session information of the visitor
      sessionCompletedOn: Date/Time of when the session was completed
      selectedChallenge: the id of challenge that might have been selected
      questions: list of question submission made in the session. Structure is same as questions, except for visitorId key will be missing
      finalText: finally submitted text (User could have changed the value)
```

* __visitorSessionStartOver__: This collection stores information regarding any incomplete sessions that user made. Session information is added when reset interaction is performed. The structure is same as visitor session

* __interactionLog__: This collection stores information regarding any interaction performed by the user. Interaction log is used to support back interaction

```
      visitorId: Session information of the visitor
      log: List of interactions done with the system
           {type: type of interaction, previousChallenge : previously selected challenge, previous_submission_id : id of previous submission which is overwritten}
```

#### Test Cases



---------------------------------------

#### Admin Site Description