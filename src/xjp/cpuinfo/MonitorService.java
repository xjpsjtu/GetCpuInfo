package xjp.cpuinfo;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.util.StringTokenizer;

public class MonitorService implements IMonitorService{
	private static final int CPUTIME = 30;
	private static final int PERCENT = 100;
	private static final int FAULTLENGTH = 10;
	private static final File versionFile = new File("/proc/version");
	private static String linuxVersion = null;
	
	/*
	 * ��ȡ��ǰ��ض���
	 */
	@Override
	public MonitorInfoBean getMonitorInfoBean() throws Exception {
		// TODO Auto-generated method stub
		int kb = 1024;
		//��ʹ���ڴ�
		long totalMemory = Runtime.getRuntime().totalMemory() / kb;
		//ʣ���ڴ�
		long freeMemory = Runtime.getRuntime().freeMemory() / kb;
		//����ʹ���ڴ�
		long maxMemory = Runtime.getRuntime().maxMemory() / kb;
		
		OperatingSystemMXBean osmxb = (OperatingSystemMXBean)ManagementFactory.getOperatingSystemMXBean();
		//����ϵͳ
		String osName = System.getProperty("os.name");
		
		double cpuRatio = 0;
		if(osName.toLowerCase().startsWith("windows")){
			cpuRatio = this.getCpuRatioForWindows();
		}else{
			cpuRatio = getCpuRateForLinux();
		}
		MonitorInfoBean infoBean = new MonitorInfoBean();
		infoBean.setFreeMemory(freeMemory);
		infoBean.setMaxMemory(maxMemory);
		infoBean.setOsName(osName);
		infoBean.setTotalMemory(totalMemory);
		infoBean.setCpuRatio(cpuRatio);
		return infoBean;
	}
	
	private double getCpuRatioForWindows(){
		try{
			String procCmd = System.getenv("windir")   
                    + "\\system32\\wbem\\wmic.exe process get Caption,CommandLine,"   
                    + "KernelModeTime,ReadOperationCount,ThreadCount,UserModeTime,WriteOperationCount";
			long[] c0 = readCpu(Runtime.getRuntime().exec(procCmd));
			Thread.sleep(CPUTIME);
			long[] c1 = readCpu(Runtime.getRuntime().exec(procCmd));
			if(c0 != null && c0 != null){
				long idletime = c1[0] - c0[0];
				long busytime = c1[1] - c0[1];
				return Double.valueOf(PERCENT * (busytime) / (busytime + idletime)).doubleValue();
			}else{
				return 0.0;
			}
		}catch(Exception e){
			e.printStackTrace();
			return 0.0;
		}
	}
	
	private long[] readCpu(final Process proc) {   
        long[] retn = new long[2];   
        try {   
            proc.getOutputStream().close();   
            InputStreamReader ir = new InputStreamReader(proc.getInputStream());   
            LineNumberReader input = new LineNumberReader(ir);   
            String line = input.readLine();   
            if (line == null || line.length() < FAULTLENGTH) {   
                return null;   
            }   
            int capidx = line.indexOf("Caption");   
            int cmdidx = line.indexOf("CommandLine");   
            int rocidx = line.indexOf("ReadOperationCount");   
            int umtidx = line.indexOf("UserModeTime");   
            int kmtidx = line.indexOf("KernelModeTime");   
            int wocidx = line.indexOf("WriteOperationCount");   
            long idletime = 0;   
            long kneltime = 0;   
            long usertime = 0;   
            while ((line = input.readLine()) != null) {   
                if (line.length() < wocidx) {
                    continue;   
                }   
                // �ֶγ���˳��Caption,CommandLine,KernelModeTime,ReadOperationCount,   
                // ThreadCount,UserModeTime,WriteOperation   
                String caption = Bytes.substring(line, capidx, cmdidx - 1)   
                        .trim();   
                String cmd = Bytes.substring(line, cmdidx, kmtidx - 1).trim();   
                if (cmd.indexOf("wmic.exe") >= 0) {   
                    continue;   
                }   
                // log.info("line="+line);   
                if (caption.equals("System Idle Process")   
                        || caption.equals("System")) {   
                    idletime += Long.valueOf(   
                            Bytes.substring(line, kmtidx, rocidx - 1).trim())   
                            .longValue();   
                    idletime += Long.valueOf(   
                            Bytes.substring(line, umtidx, wocidx - 1).trim())   
                            .longValue();   
                    continue;   
                }   
  
                kneltime += Long.valueOf(   
                        Bytes.substring(line, kmtidx, rocidx - 1).trim())   
                        .longValue();   
                usertime += Long.valueOf(   
                        Bytes.substring(line, umtidx, wocidx - 1).trim())   
                        .longValue();   
            }   
            retn[0] = idletime;   
            retn[1] = kneltime + usertime;   
            return retn;   
        } catch (Exception ex) {   
            ex.printStackTrace();   
        } finally {   
            try {   
                proc.getInputStream().close();   
            } catch (Exception e) {   
                e.printStackTrace();   
            }   
        }   
        return null;   
    }
	private static double getCpuRateForLinux(){   
        InputStream is = null;   
        InputStreamReader isr = null;   
        BufferedReader brStat = null;   
        StringTokenizer tokenStat = null;   
        linuxVersion = getLinuxVersion();
        try{   
            System.out.println("Get usage rate of CUP , linux version: "+linuxVersion);   
  
            Process process = Runtime.getRuntime().exec("top -b -n 1");   
            is = process.getInputStream();                     
            isr = new InputStreamReader(is);   
            brStat = new BufferedReader(isr);   
             
            if(linuxVersion.equals("3.1")){   
                brStat.readLine();   
                brStat.readLine();     
                 
                tokenStat = new StringTokenizer(brStat.readLine());   
                tokenStat.nextToken();   
                String user = tokenStat.nextToken();   
                tokenStat.nextToken();   
                String system = tokenStat.nextToken();   
                tokenStat.nextToken();   
                String nice = tokenStat.nextToken();   
                 
                System.out.println(user+" , "+system+" , "+nice);   
                 
//                user = user.substring(0,user.indexOf("%"));   
//                system = system.substring(0,system.indexOf("%"));   
//                nice = nice.substring(0,nice.indexOf("%"));   
                 
                float userUsage = new Float(user).floatValue();   
                float systemUsage = new Float(system).floatValue();   
                float niceUsage = new Float(nice).floatValue();   
                 
                return (userUsage+systemUsage+niceUsage);   
            }else{   
                brStat.readLine();   
                brStat.readLine();   
                     
                tokenStat = new StringTokenizer(brStat.readLine());   
                tokenStat.nextToken();   
                tokenStat.nextToken();   
                tokenStat.nextToken();   
                tokenStat.nextToken();   
                tokenStat.nextToken();   
                tokenStat.nextToken();   
                tokenStat.nextToken();   
                String cpuUsage = tokenStat.nextToken();   
                     
                 
//                System.out.println("CPU idle : "+cpuUsage);   
                Float usage = new Float(cpuUsage.substring(0, 4));   
                 
                return (100 - usage.floatValue());   
            }   
  
              
        } catch(IOException ioe){   
            System.out.println(ioe.getMessage());   
            freeResource(is, isr, brStat);   
            return 1;   
        } finally{   
            freeResource(is, isr, brStat);   
        }   
  
    }   

    private static String getLinuxVersion(){
	InputStream is = null;   
        InputStreamReader isr = null;   
        BufferedReader brStat = null;   
        StringTokenizer tokenStat = null;  
	try {
	    Process process = Runtime.getRuntime().exec("cat /proc/version");
	    is = process.getInputStream();
	    isr = new InputStreamReader(is);
	    brStat = new BufferedReader(isr);
	    tokenStat = new StringTokenizer(brStat.readLine());
	    tokenStat.nextToken();
	    tokenStat.nextToken();
	    String version = tokenStat.nextToken();
	    return version.substring(0, 3);
	} catch (IOException e) {
	    // TODO Auto-generated catch block
	    e.printStackTrace();
	    return null;
	}
    }
    private static void freeResource(InputStream is, InputStreamReader isr, BufferedReader br){   
        try{   
            if(is!=null)   
                is.close();   
            if(isr!=null)   
                isr.close();   
            if(br!=null)   
                br.close();   
        }catch(IOException ioe){   
            System.out.println(ioe.getMessage());   
        }   
    }  
    
    public static void main(String[] args) throws Exception{
    	CpuThread[] t = new CpuThread[1000];
    	for(int i = 0; i < 1000; i++){
    		t[i] = new CpuThread();
    		t[i].start();
    	}
    	DetectCpu de = new DetectCpu();
    	de.start();
    }
}

