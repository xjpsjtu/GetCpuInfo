package xjp.cpuinfo;

public class DetectCpu extends Thread{
	IMonitorService service;
	public void run(){
		while(true){
			try{
				service = new MonitorService();
				MonitorInfoBean monitorInfo = service.getMonitorInfoBean();
		    	System.out.println("cpuռ����=" + monitorInfo.getCpuRatio());
		    	this.sleep(20);
			}catch(Exception e){
				e.printStackTrace();
			}
		}
	}
}
