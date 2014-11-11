package net.ykLab.soft;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.Console;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.nio.file.*;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.zip.GZIPOutputStream;
import java.nio.file.WatchEvent.Kind;

import org.mozilla.universalchardet.UniversalDetector;

public class Main implements Runnable {
	private HashMap<String, Boolean> flags;
	private String[] pathIO;
	// http://d.hatena.ne.jp/seraphy/20120506/p2
    private int waitCnt;
    private WatchEvent.Modifier[] extModifiers;
    
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		String[] pathIO = {"./", "./"};
		HashMap<String, Boolean> flags = new HashMap<>();
		int waitCnt = 0;
        WatchEvent.Modifier[] extModifiers = new WatchEvent.Modifier[0];
        for (String arg: args){
			if(arg.startsWith("--")){
				if(arg.equals("--watch")){
					flags.put("watch", true);
				}
			} else if(arg.startsWith("-w:")) {
				waitCnt = Integer.parseInt(arg.substring(3));
			} else if((Pattern.compile(":").matcher(arg).find())) {
				pathIO = arg.split(":",2);
			}
		}
		
		System.out.println("input-> "+pathIO[0]);
		System.out.println("output->"+pathIO[1]);
		
	    File dir = new File(pathIO[0]);
	    String[] files = dir.list();
	    if(files != null){
		    for (String filename: files){
	            if(filename.endsWith(".js")){
	            	minify(pathIO[0]+"/"+filename, pathIO[1]+"/"+filename);
	            }
		    }
	    }
	    

	    if((flags != null || !flags.isEmpty()) && flags.containsKey("watch") && flags.get("watch")){
	        // �X���b�h�̊J�n
		    Main inst = new Main();
	        inst.pathIO = pathIO;
	        inst.flags = flags;
	        inst.waitCnt = waitCnt;
	        inst.extModifiers = extModifiers;
	        Thread thread = new Thread(inst);
	        thread.start();
	        
	        // �G���^�[�L�[���������܂Ŏ��s(�R���\�[��������ꍇ)
	        Console cons = System.console();
	        if (cons != null) {
	            cons.printf("�G���^�[�L�[�ŏI��.\n");
	            cons.readLine();
	 
	            // �X���b�h�ւ̏I���v���ƏI���ҋ@
	            thread.interrupt();
	        }
	 
	        // �X���b�h�I���܂őҋ@
	        try {
				thread.join();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	    }
        System.out.println("done.");
	}

	@Override
	@SuppressWarnings({"SleepWhileInLoop", "CallToThreadDumpStack"})
	public void run() {
		// TODO Auto-generated method stub
		try {
            // �t�@�C���Ď��Ȃǂ̋@�\�͐V����NIO2�N���X�Ŋg�����ꂽ�̂�
            // ��File�^����A�V����Path�^�ɕϊ�����.
            Path dirPath = new File(pathIO[0]).toPath();
            System.out.println(String.format("�Ď���: %s\n�ҋ@����: %d\n", pathIO[0], waitCnt));

            // �f�B���N�g����������t�@�C���V�X�e���𓾂�
            FileSystem fs = dirPath.getFileSystem();

            // �t�@�C���V�X�e���ɑΉ�����Ď��T�[�r�X���\�z����.
            // (��̃T�[�r�X�ŕ����̊Ď����\)
            try (WatchService watcher = fs.newWatchService())
            {
                // �f�B���N�g���ɑ΂��ĊĎ��T�[�r�X��o�^����.
                WatchKey watchKey = dirPath.register(watcher, new Kind[]{
                    StandardWatchEventKinds.ENTRY_CREATE, // �쐬
                    StandardWatchEventKinds.ENTRY_MODIFY, // �ύX
                    StandardWatchEventKinds.ENTRY_DELETE, // �폜
                    StandardWatchEventKinds.OVERFLOW},    // ����s�\��
                    extModifiers); // �I�v�V�����̏C���q�A�s�v�Ȃ�΋�z��

                // �Ď����L���ł��邩����A���[�v����.
                // (�Ď���cancel����邩�A�Ď��T�[�r�X����~�����ꍇ��false�ƂȂ�)
                while (watchKey.isValid()) {
                    try{
                        // �X���b�h�̊��荞�� = �I���v���𔻒肷��.
                        if (Thread.currentThread().isInterrupted()) {
                            throw new InterruptedException();
                        }
                        
                        // �t�@�C���ύX�C�x���g����������܂őҋ@����.
                        WatchKey detecedtWatchKey = watcher.poll(500, TimeUnit.MILLISECONDS);
                        if (detecedtWatchKey == null) {
                            // �^�C���A�E�g
                            //System.out.print(".");
                            continue;
                        }
                        System.out.println();

                        // �C�x���g�������𔻒肷��
                        if (detecedtWatchKey.equals(watchKey)) {
                            // ���������C�x���g���e���v�����g����.
                            for (WatchEvent event : detecedtWatchKey.pollEvents()) {
                                // �ǉ��E�ύX�E�폜�Ώۂ̃t�@�C�����擾����.
                                // (�������Aoverflow���Ȃǂ�null�ƂȂ邱�Ƃɒ���)
                                Path file = (Path) event.context();
                                System.out.println(event.kind() +
                                        ": count=" + event.count() +
                                        ": path=" + file);
                                if(file.toString().endsWith(".js")){
	                                if(event.kind() != StandardWatchEventKinds.ENTRY_DELETE){
	                                	minify(pathIO[0]+"/"+file.toString(), pathIO[1]+"/"+file.toString());
	                                }
                                }
                            }
                        }

                        // �C�x���g�̃n���h�����O�Ɏ��Ԃ�������P�[�X��
                        // Sleep�ŃG�~�����[�g����.
                        // (���̊Ԃ̃t�@�C���ύX�C�x���g����肱�ڂ����ۂ����m���߂���)
                        for (int cnt = 0; cnt < waitCnt; cnt++) {
                            System.out.print(String.format("%d/%d...\r", cnt + 1, waitCnt));
                            Thread.sleep(1000);
                        }

                        // �C�x���g�̎�t���ĊJ����.
                        detecedtWatchKey.reset();
                        
                    } catch (InterruptedException ex) {
                        // �X���b�h�̊��荞�� = �I���v���Ȃ̂ŊĎ����L�����Z�������[�v���I������.
                        System.out.println("�Ď��̃L�����Z��");
                        watchKey.cancel();
                    }
                }
            }
	     } catch (RuntimeException | IOException ex) {
	    	 ex.printStackTrace();
	     }
	     System.out.println("�X���b�h�̏I��");
	}
	
	private static void minify(String filename_from, String filename_to){
		// http://www.atmarkit.co.jp/fjava/javatips/069java006.html
		String urlString = "http://closure-compiler.appspot.com/compile";
		File file_from = new File(filename_from);
		File file_to = new File(filename_to);
		try {
			//BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(file_from),(detectFileEncoding(file_from) != null)?detectFileEncoding(file_from):"UTF-8"));
			BufferedReader br;
			System.out.print(filename_from+" minifing now  --> ");
			if(detectFileEncoding(file_from) == null){
				br = new BufferedReader(new InputStreamReader(new FileInputStream(file_from)));
			}else if(detectFileEncoding(file_from).toUpperCase().equals("UTF-8")){
				br = openTextFileR(filename_from,"utf-8");
			}
			else{
				br = new BufferedReader(new InputStreamReader(new FileInputStream(file_from),(detectFileEncoding(file_from) != null)?detectFileEncoding(file_from):"UTF-8"));
			}
			String source = "";
			String str;
			while((str = br.readLine()) != null){
				//source += str.replaceAll("\r\n", "\n");
				source += str+"\n";
			}
			  
            URL url = new URL(urlString);
            URLConnection uc = url.openConnection();
            uc.setDoOutput(true);//POST�\�ɂ���

            uc.setRequestProperty("User-Agent", "Javascript-minify&compress");// �w�b�_��ݒ�
            uc.setRequestProperty("Accept-Language", "ja");// �w�b�_��ݒ�
            uc.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");// �w�b�_��ݒ�
            OutputStreamWriter os = new OutputStreamWriter(uc.getOutputStream(), "utf-8");//POST�p��OutputStream���擾
            String postStr = "compilation_level=ADVANCED_OPTIMIZATIONS&output_format=text&output_info=compiled_code&js_code="+URLEncoder.encode(source, "utf-8");//POST����f�[�^
//            String postStr = "input="+source;//POST����f�[�^
            BufferedWriter ps = new BufferedWriter(os);
            ps.write(postStr);//�f�[�^��POST����
            ps.close();

            InputStreamReader is = new InputStreamReader(uc.getInputStream(), "utf-8");//POST�������ʂ��擾
            BufferedReader reader = new BufferedReader(is);
            String s;
            try{
                if (checkBeforeWritefile(file_to)){
	                PrintWriter pw = new PrintWriter(new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file_to),"UTF-8")));
	  	            while ((s = reader.readLine()) != null) {
		                pw.print(s);
		            }
	  	            pw.close();
	  	            System.out.println("Success!");
	  	            gzipCompress(filename_to, filename_to+".gz");
                }else{
                	System.out.println("�t�@�C���ɏ������߂܂���");
                }
			}catch(IOException e){
				System.out.println(e);
			}
            reader.close();

        } catch (MalformedURLException e) {
            System.err.println("Invalid URL format: " + urlString);
            System.exit(-1);
        }catch (FileNotFoundException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
            System.err.println("Can't connect to " + urlString);
            System.exit(-1);
        }catch(Exception e){
            e.printStackTrace(System.err);
            }
	}
	private static boolean checkBeforeWritefile(File file){
		if (file.exists()){
			if (file.isFile() && file.canWrite()){
		        return true;
		    }
		}
		try {
			file.createNewFile();
			if (file.isFile() && file.canWrite()){
		        return true;
		    }
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		}
		return false;
	}
	
	// http://pgcafe.moo.jp/JAVA/file/main_1.htm
	public static void gzipCompress(String from, String to) throws IOException {
	    byte[] buf = new byte[1024];
	    System.out.print(from+" compressing gzip ---> ");
	    //���k���t�@�C���ւ̃X�g���[�����J��
	    BufferedInputStream in = new BufferedInputStream(new FileInputStream(from));
	    //���k��t�@�C���ւ̃X�g���[�����J��
	    GZIPOutputStream out = new GZIPOutputStream(new FileOutputStream(to));
	    //�f�[�^�����k���ď�������
	    int size;
	    while ((size = in.read(buf, 0, buf.length)) != -1) {
	            out.write(buf, 0, size);         
	    }
	    out.flush();
	    out.close();
	    in.close();     
	    System.out.println("Success!");
	}
	
	public static String detectFileEncoding(File file) throws IOException  {
	    String result = null;
	    byte[] buf = new byte[4096];
	    FileInputStream fis = new FileInputStream(file);
	    UniversalDetector detector = new UniversalDetector(null);
	    
	    int nread;
	    while ((nread = fis.read(buf)) > 0 && !detector.isDone()) {
	        detector.handleData(buf, 0, nread);
	    }
	    detector.dataEnd();
	    
	    result =  detector.getDetectedCharset();
	    detector.reset();
	    
	    return result;
	}
	
	// http://k-hiura.cocolog-nifty.com/blog/2013/03/javautf-8bom-dd.html
	/** �e�L�X�g�t�@�C����ǂݍ��ݗp�ɃI�[�v������ */
	public static BufferedReader openTextFileR(
			String fileName,String charSet)
					throws Exception{
		return new BufferedReader(
				new InputStreamReader(
						skipUTF8BOM(
								new FileInputStream(
										new File(fileName))
								,charSet)
								,charSet)
				);
		}
	/** UTF-8��BOM���X�L�b�v���� */
	public static InputStream skipUTF8BOM(
			InputStream is
			,String charSet
			)throws Exception{
		if( !charSet.toUpperCase().equals("UTF-8") ) return is;
		if( !is.markSupported() ){
			// �}�[�N�@�\�������ꍇBufferedInputStream��킹��
			is= new BufferedInputStream(is);
			}
		is.mark(3); // �擪�Ƀ}�[�N��t����
		if( is.available()>=3 ){
			byte b[]={0,0,0};
			is.read(b,0,3);
			if( b[0]!=(byte)0xEF ||
					b[1]!=(byte)0xBB ||
					b[2]!=(byte)0xBF ){
				is.reset();// BOM�łȂ��ꍇ�͐擪�܂Ŋ����߂�
				}
			}
		return is;
		}
	}
