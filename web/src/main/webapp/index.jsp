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
					<form method="post" action="data-io" enctype="multipart/form-data">
						Questions (Please select a CSV file containing questions): 
						<input type="file" name="questionCSV" />
						<br/>
						<input type="submit" name="importQuestions" value="Import Questions" />
					</form>
				</li>
				<li>
					<form method="post" action="data-io" enctype="multipart/form-data">
						Challenges (Please select a CSV file containing challenges): 
						<input type="file" name="challengeCSV" />
						<br/>
						<input type="submit" name="importChallenges" value="Import Challenges" />
					</form>
				</li>
				<li>
					<form method="get" action="data-io">
						Export Questions <input type="submit" name="exportQuestions" value="Export" />
					</form>
				</li>
				<li>
					<form method="get" action="data-io">
						Export Challenges <input type="submit" name="exportChallenges" value="Export" />
					</form>
				</li>
				<li>
					<form method="post" action="data-io">
						Clear All Documents <input type="submit" name="clearAll" value="Clear All Documents" />
					</form>
				</li>
			</ul>
	</div>
</body>
</html>
