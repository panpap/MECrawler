hartool="java -jar hartools/hartools.jar "
tempFile=".temp.csv"
path=ARGV[0]
abort "Input folder is needed..." if path==nil
path+="/" if path[-1, 1]!='/'
out=path+"outCSV/"
Dir.mkdir out if not Dir.exist? out
for file in Dir.entries(path).select {|f| !File.directory? f}
	puts file
	system(hartool+" --in "+path+file+" --out "+tempFile)
	system("awk -F'\t' '{if (NR!=1) {print -1\"\t\"1\"\t\"$1\"\t\"$6\"\t\"$3\"\t\"$4\"\t\"($7+$8+$9)\"\t\"$5\"\t\"$2\"\t\"$10\"\t\"$11\"\t\"$12\"\t\"$13\"\t\"$14\"\t\"$15\"\t\"$16\"\t\"$17}}' .temp.csv > "+out+file.gsub(".har",".csv"))
	system("rm -f "+tempFile)
end
