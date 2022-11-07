package com.dinesh.response;

import lombok.Data;

@Data
public class CoResponse {

	private Long totolTriggers;
	
	private Long succTriggers;
	
	private Long failedTrigger;
}
