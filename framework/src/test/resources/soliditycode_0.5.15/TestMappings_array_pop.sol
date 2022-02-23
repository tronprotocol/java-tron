contract C {
	mapping (uint256 => uint256)[] a;

	function n1(uint256 key, uint256 value) public {
		a.length++;
		a[a.length - 1][key] = value;
	}



	function map(uint256 key) public view returns (uint) {
		return a[a.length - 1][key];
	}

	function p() public {
		a.pop();
	}
}

