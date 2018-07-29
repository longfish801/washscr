validTags {
	dec = 'washsh'
	parent = [ 'slice', 'between' ]
	child = [ 'replace', 'reprex', 'call' ]
}

validAttrKeys = [
	'slice' : [ 'div', 'divhandle', 'judge' ],
	'between' : [ 'bgn', 'end', 'divhandle', 'judge' ],
	'replace' : [ 'judge', 'first', 'last' ],
	'reprex' : [ 'judge', 'first', 'last' ],
	'call' : [ 'combi', 'judge', 'first', 'last' ]
];

validAttrVals = [
	'divhandle' : [ 'include', 'exclude', 'delete' ]
];

