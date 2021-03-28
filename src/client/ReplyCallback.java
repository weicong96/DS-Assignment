package client;

import java.util.HashMap;

public interface ReplyCallback {
	void OnReply(HashMap<String, Object> reply);
}
