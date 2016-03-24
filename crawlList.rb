require 'optparse'

def runner(file,device,extension,throttle,dir)
	device="Apple iPhone 6" if device==nil	#default case
	command="java -Dorg.apache.logging.log4j.simplelog.StatusLogger.level=OFF -jar \"./bin/mobileEmulatorCrawler.jar\" -file \"#{file}\""
	command+=" -device \"#{device}\"" if device!=nil
	command+=" -throttle \"#{throttle}\"" if throttle!=nil
	command+=" -extension \"#{extension}\"" if extension!=nil
	command+=" -dir \"#{dir}\"" if not dir==nil
	#puts command
	system(command)
end

availableExtensions=["extensions/adblockplus-1.11.0.1580.crx","extensions/disconnect_5_18_23.crx","extensions/ghostery_5_4_11.crx"]
options = Hash.new
start = Time.now
OptionParser.new { |opts|
  	opts.banner = "Usage: crawl.rb [options]"
  	opts.on("-f", "--file FILE", "File with url list") do |v|
    	options[:file] = v
  	end
	opts.on("-d", "--directory DIRECTORY", "Change directory with resources") do |v|
    	options[:dir] = v
  	end
	opts.on("-m", "--device DEVICE", "Mobile device to emulate") do |v|
    	options[:device] = v
  	end
	opts.on("-t", "--throttle THROTTLE", "Network Throttling") do |v|
    	options[:throttle] = v
	end
	opts.on("-e", "--extension EXTENSION", "Extension to load") do |v|
		availableExtensions.each{|ext| 
			ext=dir+ext if options[:dir]!=nil
			(options[:extensions]=ext; break) if ext.include? v}
  	end
}.parse!
abort "ERROR: No URL list was given!" if options[:file]==nil
runner(options[:file],options[:device],options[:extensions],options[:throttle],options[:dir])
finish = Time.now
puts "Total Elapsed time "+(finish - start).to_s+" seconds"
