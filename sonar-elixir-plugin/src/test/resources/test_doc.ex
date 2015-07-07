defmodule Docs do
	@moduledoc "This is docs module"

	@doc """
	function doc1
	"""
	def doc1() do
	end

	@doc false
	def doc2() do
	end

	@doc "doc3 function"
	def doc3() do
	end

	@doc nil 
	def doc4() do
	end
end
