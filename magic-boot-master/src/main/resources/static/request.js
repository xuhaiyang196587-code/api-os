var Request = {
	post: function(url, data, callback, async) {
		$.ajax({
			url: url,
			type: 'POST',
			data: data || {},
			async: async || false,
			dataType: 'json',
			success: function(res) {
				if (callback) {
					callback(res);
				}
			},
			error: function(jqXHR, _textStatus, _errorThrow) {
				if (callback) {
					callback(JSON.parse(jqXHR.responseText));
				}
			}
		});
	},
	get: function(url, data, callback, async) {
		$.ajax({
			url: url,
			type: 'get',
			data: data || {},
			async: async || false,
			dataType: 'json',
			success: function(res) {
				if (callback) {
					callback(res);
				}
			},
			error: function(jqXHR, _textStatus, _errorThrow) {
				if (callback) {
					callback(JSON.parse(jqXHR.responseText));
				}
			}
		});
	},
	readFileContent: function(url) {
		 var response = $.ajax({
	        url: url,
	        type: 'GET',
	        dataType: 'text',
	        async: false
	    });
	    
	    if (response.status !== 200) {
	        throw new Error("请求失败，状态码: " + response.status);
	    }
	    
	    return response.responseText;
	},
	postJSON: function(url, data, callback, async) {
		$.ajax({
			url: url,
			type: 'POST',
			data: data || {},
			contentType: 'application/json',
			async: async || false,
			success: function(res) {
				if (callback) {
					callback(res);
				}
			},
			error: function(jqXHR, _textStatus, _errorThrow) {
				if (callback) {
					callback(JSON.parse(jqXHR.responseText));
				}
			}
		});
	},
	getPost: function(url, data) {
		let result = null;
		$.ajax({
			url: url,
			type: 'POST',
			data: data || {},
			async: false,
			dataType: 'json',
			success: function(res) {
				result = res;
			},
			error: function(jqXHR, _textStatus, _errorThrow) {
				result = JSON.parse(jqXHR.responseText);
			}
		});
		return result;
	}
}