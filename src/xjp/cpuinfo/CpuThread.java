package xjp.cpuinfo;

public class CpuThread extends Thread{
	int i = 0;
	public void run(){
		while(true){
			try {
				this.sleep(5);
//				System.out.println(i++);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
}
