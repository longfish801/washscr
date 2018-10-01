check {
	valids = [ 'range', 'format', 'divided', 'enclosed', 'complex', 'replace', 'reprex', 'call', 'delete' ]
	hierarchy = [
		'washsh' : [ 'range', 'format' ],
		'range': [ 'divided', 'enclosed', 'complex' ],
		'format': [ 'replace', 'reprex', 'call', 'delete' ]
	]
}
