check {
	valids = [ 'range', 'format', 'mask', 'divided', 'enclosed', 'tree', 'replace', 'reprex', 'call' ]
	hierarchy = [
		'washsh' : [ 'range', 'format' ],
		'range': [ 'mask', 'divided', 'enclosed', 'tree' ],
		'format': [ 'replace', 'reprex', 'call' ]
	]
}
