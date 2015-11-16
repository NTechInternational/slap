SLAP Documentation


	[Selected Mode]
Get Responded Variables



   [NoTransaction Type] 
       |
       |
       |  
       --------> Previously Selected Questions ---> Select Interaction
       |
       |
       |
       --------> if facets have been previously selected ---> Filter based on facets --> one row
       |
       |
       |-------> else filter query = BusinessModel : All with single row


    [Reset Transaction Type]
       |
       |-------> a. Save all the submitted question to the incomplete session history
       			 b. Remove all interaction log (useful for back interaction)
       			 c. Clear selected challenge
       			 d. Clear previously submitted questions
       			 e. perform default interaction

    [Done Transaction Type]
    	|
    	|------> a. Save all the submitted question to complete session history
    			 b. Save final text (Get Param: text) along with history
    			 c. Remove all interaction log
    			 d. Clear previously submitted questions
    			 e. perform default interaction


    [Submit Transaction Type]
    	|
		|------> a. get all facets and variables submission for the question
		         b. save the variable and question


	[Select Transaction Type]
		|
		|------> a. 




two queries


questions
  : selected using 
challenges