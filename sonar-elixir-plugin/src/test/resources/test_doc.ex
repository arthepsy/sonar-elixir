defmodule Docs do
	@moduledoc "This is docs module"

	@doc """
	function doc1

	documentation
	"""
	def doc1() do
	end

	@doc false
	def doc2() do
	end

	@doc "doc3 function"
	def doc3() do
	end

	@doc "private function doc4"
	defp doc4() do
	end

	@doc nil 
	def doc5() do
	end

	# very
	#  very
	#   private function doc6
	defp doc6() do
	end

	@typedoc "type doc example"
	@type doc7 :: any

end
