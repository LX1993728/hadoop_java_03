package liuxun.hadoop.mr.ii;

import java.io.IOException;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.InputSplit;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.input.FileSplit;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;

/**
 * 反向索引
 * 
 * @author liuxun
 *
 */
public class InverseIndex {
	public static class IndexMapper extends Mapper<LongWritable, Text, Text, Text>{
		private Text k = new Text();
		private Text v = new Text();
		@Override
		protected void map(LongWritable key, Text value,Context context)
				throws IOException, InterruptedException {
			String line = value.toString();
			String[] words = line.split(" ");
			// 可以从context中获取当前读取输入切片的信息
			FileSplit inputSplit = (FileSplit) context.getInputSplit();
			String path = inputSplit.getPath().toString();// 格式是:hdfs://hostName:9000/directory ../filename
			// 获取截取的部分 /directory.../filename
			for (String word : words) {
				k.set(word+"->"+path);
				v.set("1");
				context.write(k, v);
			}
			
		}
	}
	
	public static class IndexCombiner extends Reducer<Text, Text, Text, Text>{
		private Text k = new Text();
		private Text v = new Text();
		@Override
		protected void reduce(Text key, Iterable<Text> values, Context context)
				throws IOException, InterruptedException {
			String[] wordAndPath = key.toString().split("->");
			String word = wordAndPath[0];
			String path = wordAndPath[1];
			int counter = 0;
			for (Text t : values) {
				counter += Integer.parseInt(t.toString());
			}
			k.set(word);
		    v.set(path+"->"+counter);	
			context.write(k,v);
		}
	}
	public static class IndexReducer extends Reducer<Text, Text, Text, Text>{
		private Text v = new Text();
		@Override
		protected void reduce(Text key, Iterable<Text> values, Context context)
				throws IOException, InterruptedException {
			StringBuilder resultBuilder = new StringBuilder();
			for (Text t : values) {
				resultBuilder.append((t.toString()+"\t").toCharArray());
			}
			v.set(resultBuilder.toString());
			context.write(key,v );
		}
	}
	public static void main(String[] args) throws Exception {
		Configuration conf  = new Configuration();
		Job job  =Job.getInstance(conf);
		job.setJarByClass(InverseIndex.class);
		
		job.setMapperClass(IndexMapper.class);
		job.setMapOutputKeyClass(Text.class);
		job.setMapOutputValueClass(Text.class);
		FileInputFormat.setInputPaths(job, new Path(args[0]));
		
		job.setCombinerClass(IndexCombiner.class);
		job.setReducerClass(IndexReducer.class);
		job.setOutputKeyClass(Text.class);
		job.setOutputValueClass(Text.class);
		FileOutputFormat.setOutputPath(job, new Path(args[1]));

		job.waitForCompletion(true);
	}
}
