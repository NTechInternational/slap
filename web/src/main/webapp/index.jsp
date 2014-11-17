<html>
<head>
	<title>Slap Admin Portal</title>
	<style type="text/css">
		body { font: 14px/1.5 Lato, sans-serif; }
		h2{color: rgb(0,77,74); vertical-align: top; font: 24px/1.5 Lato, sans-serif; text-transform: uppercase;
		text-align:center;}
		ul { list-style: none; padding: 0}
		
		.slap-challenge-item {
			background: rgb(0, 214, 210);
			padding-bottom: 1em;
			font-size: 110%;
			margin-top:20px;
		}
		.slap-challenge-item:first-child {
			margin-top: 0;
		}
		.slap-challenge-item:last-child {
			margin-bottom: 0;
		}
		
		.slap-challenge-button {
			height: 28px;
			padding: 0 0.8em;
			cursor: pointer;
			text-align: center;
			color: #fff;
			background: rgb(237,115,0);
			border: 0;
			-webkit-border-radius: 3px;
			-moz-border-radius: 3px;
			border-radius: 3px;
			font-size: 90%;
			box-shadow: 0 2px 0 rgb(205,111,0);
			width:200px;
		}
		
		.slap-challenge-label {
			text-indent: 20px;
			text-transform: uppercase;
			font-size: 125%;
			color: rgb(204,255,255);
		}
		
		.slap-challenge-top {
			color: #fff;
			background: rgb(0, 181, 176);
			border-bottom: 3px solid rgb(101, 235, 231);
			line-height: 47px;
			position: relative;
		}
		
		.slap-challenge-select {
			position: absolute;
			right: 20px;
			top: 9px;
		}
		
		.slap-challenge-body {
			padding: 5px 25px 0;
			padding-top:25px;
		}
		
		.slap-challenge-body input {margin-left: 15px;}
	</style>
</head>
<body>
	<h2>Slap Admin Portal</h2>
	<div id="allActions">
		
			<ul>
				<li class="slap-challenge-item">
					<form method="post" action="data-io" enctype="multipart/form-data">
						<div class="slap-challenge-top">
							<div class="slap-challenge-label">Questions</div>
							<input type="submit" class="slap-challenge-button slap-challenge-select" name="importQuestions" value="Import Questions" />
						</div>
						<div class="slap-challenge-body">
							Please select a CSV file containing questions: <input type="file" name="questionCSV" />
						</div>
						
					</form>
				</li>
				<li class="slap-challenge-item">
					<form method="post" action="data-io" enctype="multipart/form-data">
						<div class="slap-challenge-top">
							<div class="slap-challenge-label">Challenges</div>
							<input type="submit" class="slap-challenge-button slap-challenge-select" name="importChallenges" value="Import Challenges"  />
						</div>
						<div class="slap-challenge-body">
							Please select a CSV file containing challenges: <input type="file" name="challengeCSV" />
						</div>
					</form>
				</li>
				<li class="slap-challenge-item">
					<form method="get" action="data-io">
						<div class="slap-challenge-top">
							<div class="slap-challenge-label">Export Questions</div>
							<input type="submit" class="slap-challenge-button slap-challenge-select" name="exportQuestions" value="Export"  />
						</div>
						<div class="slap-challenge-body">
							Click on export to download all the questions in the database.
						</div>
					</form>
				</li>
				<li class="slap-challenge-item">
					<form method="get" action="data-io">
						<div class="slap-challenge-top">
							<div class="slap-challenge-label">Export Challenges</div>
							<input type="submit" class="slap-challenge-button slap-challenge-select" name="exportChallenges" value="Export"  />
						</div>
						<div class="slap-challenge-body">
							Click on export to download all the challenges in the database.
						</div>
					</form>
				</li>
				<li class="slap-challenge-item">
					<form method="post" action="data-io" onsubmit="return confirm('Are you sure you want to remove all questions and challenges from the database?')">
						<div class="slap-challenge-top">
							<div class="slap-challenge-label">Clear All Documents</div>
							<input type="submit" class="slap-challenge-button slap-challenge-select" name="clearAll" value="Clear All Documents"  />
						</div>
						<div class="slap-challenge-body">
							Click to remove all the data (questions and challenges) from the database. NOTE: This action can't be undone. 
						</div>
					</form>
				</li>
			</ul>
	</div>
</body>
</html>
