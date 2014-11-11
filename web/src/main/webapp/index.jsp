<%@ page isELIgnored="false"%>
<html>
<head>
	<title>Slap Admin Portal</title>
</head>
<body>
	<h2>Slap Admin Portal</h2>
	<div>
		<h3>Actions</h3>
		
			<ul>
				<li>
				<form method="post" action="upload" enctype="multipart/form-data">
					Questions (Please select a CSV file containing questions): 
					<input type="file" name="questionCSV" />
					<br/>
					<input type="submit" name="importQuestions" value="Import Questions" />
				</form>
				</li>
			</ul>
	</div>
</body>
</html>
