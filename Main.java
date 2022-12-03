import java.io.*;
import java.net.*;

class Request
{
    String url,method,version,data,Content_Length;
    Request()
    {
        url="";
        method="";
        version="";
        data="";
        Content_Length="";
    }
}


class Response
{
    String Content_Type,Content_Length,code;
    String version,instruc;
    byte[] data;
    Response()
    {
        code="-100";
        Content_Type="";
        Content_Length="";
        version="";
        instruc="";
        data=new byte[1<<12];
    }

    byte[] header_content()
    {
        try{
            String ret=version+" "+code+" "+instruc+"\r\n";
            ret+="Content_Type:"+Content_Type+"\r\n";
            ret+="Content_Length:"+Content_Length+"\r\n\r\n";
            return ret.getBytes();
        }catch (Exception e){
            ;
        }
        return null;
    }
}

class Server extends Thread
{
    InputStream in;
    OutputStream out;
    String root;
    int BUFFER_SIZE=1024;
    Server(Socket s,String p)
    {
        try
        {
            in=s.getInputStream();
            out=s.getOutputStream();
            root=p;
        }catch(IOException e)
        {
            e.printStackTrace();
        }
    }

    Request get()
    {
        BufferedReader reader=new BufferedReader(new InputStreamReader(in));
        Request request=new Request();
        try{
            String line=reader.readLine();

            System.out.println(line);

            request.method=line.substring(0,line.indexOf(" "));
            line=line.substring(line.indexOf(" ")+1);
            request.url=line.substring(0,line.indexOf(" "));
            line=line.substring(line.indexOf(" ")+1);
            request.version=line;
            boolean flag=false;
            line=reader.readLine();
            while(!line.equals(""))
            {
                String key=line.substring(0,line.indexOf(":"));
                String value=line.substring(line.indexOf(":")+2);
                if(key.equals("Content-Length"))//用于post处理
                {
//                    System.out.println("::::this is lenth"+key+value);
                    flag=true;
                    request.Content_Length=value;
                }
                line=reader.readLine();
            }
            if(flag)
            {
                int length=Integer.parseInt(request.Content_Length);
                while(length>0)
                {
                    request.data += (char)reader.read();
                    length--;
                }
            }
        }catch(Exception e)
        {
            e.printStackTrace();
        }
        return request;
    }

    void reply(Request request) throws IOException
    {

        if(request.method.equals("GET"))
        {
            File reply_file=new File(root+request.url);
            Response response=new Response();

            System.out.println("do get file: "+root+request.url);

            // 作为资源文件的缓冲内存
            byte[] bytes=new byte[BUFFER_SIZE];
            // 传输http头
            if(!reply_file.exists())
            {
                System.out.println("file not exist");
                response.code="404";
                response.version="HTTP/1.1";
                response.instruc="requested file not exists";
                response.Content_Length="25";
                response.Content_Type="text/html; charset=UTF-8";
            }else{
                String form=request.url.substring(request.url.lastIndexOf(".")+1);
                response.code="200";
                response.version="HTTP/1.1";
                response.instruc="requested file exists";
                response.Content_Length= String.valueOf(reply_file.length());
                if(form.equals("html"))
                {
                    System.out.println("request for html");
                    response.Content_Type="text/html; charset=UTF-8";
                }
                else if(form.equals("jpg"))
                {
                    System.out.println("request for jpg");
                    response.Content_Type="application/jpeg"; //image/jpeg
                }
                else
                {
                    System.out.println("request for plain");
                    response.Content_Type="text/plain; charset=UTF-8";
                }
            }
            out.write(response.header_content());

            // 传输数据字段
            if(!reply_file.exists()){
                bytes="<html><h1> 404 Not Found! </h1></html>".getBytes("UTF-8");
                out.write(bytes);
                out.flush();
                out.close();
                return;
            }
            InputStream freader=new FileInputStream(reply_file);
            int ch = freader.read(bytes, 0, BUFFER_SIZE);
            while (ch != -1) {
                out.write(bytes, 0, ch);
                ch = freader.read(bytes, 0, BUFFER_SIZE);
            }
            out.flush();
            out.close();
            freader.close();
            return;
        }
        else if(request.method.equals("POST"))
        {
            // 处理收到的POST请求
            String name="3200104436";
            String pwd="4436";
            Response response = new Response();
            if(!request.url.equals("/dopost"))
            {
                response.version="HTTP/1.1";
                response.code="404";
                response.instruc="url error";
                response.Content_Type="text/html; charset=UTF-8";
                response.Content_Length="25";
                response.data="<h1> 404 Not Found! </h1>".getBytes();
                out.write(response.header_content());
                out.write(response.data);
                out.flush();
                out.close();
                return;
            }
            System.out.println("received form data: "+request.data);
            if(!(request.data.contains("login") && request.data.contains("pass")))
            {
                response.data="<h1> Login Failed! </h1>".getBytes();
                response.Content_Length="24";
                response.instruc="key error";
            }
            else
            {
                String data=request.data;
                String value1=data.substring(data.indexOf("=")+1,data.indexOf("&"));
                data=data.substring(data.indexOf("&")+1);
                String value2=data.substring(data.indexOf("=")+1);
                if(value1.equals(name) && value2.equals(pwd))
                {
                    response.data="<h1> Login Success! </h1>".getBytes();
                    response.Content_Length="25";
                    response.instruc="success";
                }
                else
                {
                    response.data="<h1> Login Failed! </h1>".getBytes();
                    response.Content_Length="24";
                    response.instruc="fail";
                }
            }
            response.version="HTTP/1.1";
            response.code="200";
            response.Content_Type="text/html; charset=UTF-8";
            out.write(response.header_content());
            out.write(response.data);
            out.flush();
            out.close();
            return;
        }
    }
    public void serve()
    {
        try{
            reply(get());
        }catch(IOException e)
        {
            e.printStackTrace();
        }
    }
}


public class Main
{
    public static void main(String[] args)
    {
        try
        {
            System.out.println("----------<http-server launched>------------");
            ServerSocket socket=new ServerSocket(4436);
            while(true)
            {
                Socket skt=socket.accept();
                Server my_server=new Server(skt,"./resource"); //resource
                my_server.serve();
            }
        }catch(IOException e)
        {
            e.printStackTrace();
        }
    }
}
