package server;

import bean.Constant;
import org.htmlcleaner.HtmlCleaner;
import org.htmlcleaner.TagNode;
import org.htmlcleaner.XPatherException;
import redis.clients.jedis.Jedis;
import utils.RedisUtils;
import utils.Utils;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class getPicService {
    static List<String> urlList = new LinkedList<>();
    //开始页URl
    String getPicUrl = "https://forum.gamer.com.tw/C.php?bsn=164&snA=8634";

    public List<String> getPicList() throws XPatherException {
        String resultHtmlStr = Utils.sendGet(getPicUrl,Utils.getHeader(null),null);
        TagNode tagNode= new HtmlCleaner().clean(resultHtmlStr);
        //根据Xpath获取图片地址
        Object[] picArray = tagNode.evaluateXPath("//article[@class='c-article FM-P2']//div/div[*]//a[@class='photoswipe-image']/@href");
        System.out.println(getPicUrl+"页有图片："+picArray.length+"张");
        ArrayList<String> list = new ArrayList(Arrays.asList(picArray));
        for (int i = 0; i < list.size(); i++) {
            // 这个地址开头的地址，不是想要的地址，需要删除。
            if(list.get(i).startsWith("https://ref.gamer.com.tw")){
                list.remove(list.get(i));
                i--;
                continue;
            }
        }
        urlList.addAll(list);
        Object[] nextUrlArray = tagNode.evaluateXPath("//*[@id=\"BH-pagebtn\"]/a[2]/@href");
        //判断是否有下一页，地址是残缺的
        String nextUrl = nextUrlArray.length>0?String.valueOf(nextUrlArray[0]):null;
        getPicUrl = "https://forum.gamer.com.tw/C.php"+nextUrl;
        if(nextUrl!=null && !"".equals(nextUrl)){
//            getPicList();
        }
        return urlList;
    }

    public void saveDataToRedis(List<String> urlList){
        Jedis jedis = RedisUtils.getJedis();
        for (int i = 0; i < urlList.size(); i++) {
            jedis.set("Doax"+i, urlList.get(i));
        }
        jedis.close();
    }

    public void DownLoadPicForRedis() throws IOException {
        Jedis jedis = RedisUtils.getJedis();
        Set<String> key = jedis.keys("*");
        for (String s : key) {
            String url = jedis.get(s);
            String filePath = Utils.downloadUseHttpClient(Utils.getHeader(null),url, Constant.downLoadFilePath,s+".jpg");
            File file = new File(filePath);
            if(file.length() > 4*1024){
                jedis.del(s);
            }
        }
    }

    public void choose() throws XPatherException, IOException {
        Scanner scanner = new Scanner(System.in);
        String fileNamePrefix="Doax";
        System.out.println("==========================================");
        System.out.println("1、直接下载图片\n2、保存数据到Redis\n3.获取redis数据下载数据");
        System.out.println("==========================================");
        int choose = scanner.nextInt();
        switch (choose){
            case 1:
                List<String> urlList = getPicList();
                for (int i = 0; i < urlList.size(); i++) {
                    Utils.downloadUseHttpClient(Utils.getHeader(null),urlList.get(i), Constant.downLoadFilePath,fileNamePrefix+i+".jpg");
                }
                break;
            case 2: saveDataToRedis(getPicList()); break;
            case 3: DownLoadPicForRedis(); break;
        }
        scanner.close();
    }

}
