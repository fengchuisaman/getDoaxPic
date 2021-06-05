import org.htmlcleaner.HtmlCleaner;
import org.htmlcleaner.TagNode;
import org.htmlcleaner.XPatherException;
import redis.clients.jedis.Jedis;
import server.getPicService;
import utils.RedisUtils;
import utils.Utils;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class getPic {

    public static void main(String[] args) throws XPatherException, IOException {

        new getPicService().choose();

        System.out.println("完成");
    }

    }
