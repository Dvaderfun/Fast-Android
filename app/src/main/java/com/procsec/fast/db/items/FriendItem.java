package com.procsec.fast.db.items;
import java.util.*;
import com.procsec.fast.vkapi.model.*;

public class FriendItem {
	public VKUser friend;
	public int id;
	
	public FriendItem (VKUser friend, int id) {
		this.id = id;
		this.friend = friend;
	}
}
